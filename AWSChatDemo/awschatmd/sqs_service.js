const AWS = require("aws-sdk");
const config = require("./config.js").config;
const Utils = require("./utils.js").Utils;

let runLoop = false;
const ACTIVE_QUEUE_WAIT_MSEC = 10;

/* sqsContext = {
    SQS,
    IAM,
    runLoop,
    mainQueueURL,
    lastMessageTS
}

context = main app context (rooms and users)
*/

function loopSQS(sqsContext, context) {
    if (!sqsContext.runLoop) {
        return;
    }
    let queueTimeout = 10;
    let idleTimeOutDate = new Date().getTime() - config.loopIdleTimeoutSec * 1000;
    if (!sqsContext.lastMessageTS) {
        sqsContext.lastMessageTS = new Date().getTime();
    }
    console.log("loopSQS run " + Utils.formatDateTime(new Date()) + ", runloop=" + sqsContext.runLoop 
    + ", idleTimeout=" + Utils.formatDateTime(new Date(idleTimeOutDate))
    + ", lastMEssage=" + Utils.formatDateTime(new Date(sqsContext.lastMessageTS))
    );
    sqsContext.SQS.receiveMessage({
        QueueUrl: sqsContext.mainQueueURL,
        //AttributeNames: ["All"],
        MessageAttributeNames: ["All"],
        MaxNumberOfMessages : 10,
        WaitTimeSeconds: 10
        
    }, (err, data) => {        
        try {
            let msgs = [];
            if (err || !data) {
                msgs.push({
                    userID: 0,
                    message: err || "[No Data]",
                    TS: Utils.formatDateTime(new Date())  
                });
                if (sqsContext.lastMessageTS < new Date().getTime() - config.loopIdleTimeoutSec * 1000) {
                    sqsContext.runLoop = false;
                    console.log("stop loop, timeout=" + config.loopIdleTimeoutSec + ", deadtime=" + idleTimeOutDate);
                    console.log("lastMessageTS=" + sqsContext.lastMessageTS + ", lasttime=" + new Date(sqsContext.lastMessageTS))
                }
            } else if (!data.Messages || data.Messages.length == 0) {
                // this is normal, ignore
                if (sqsContext.lastMessageTS < new Date().getTime() - config.loopIdleTimeoutSec * 1000) {
                    sqsContext.runLoop = false;
                    console.log("stop loop-2, timeout=" + config.loopIdleTimeoutSec + ", deadtime=" + idleTimeOutDate);
                    console.log("lastMessageTS=" + sqsContext.lastMessageTS + ", lasttime=" + new Date(sqsContext.lastMessageTS))
                }
            } else {
                console.log("receiveMessage data: " + JSON.stringify(data));
                sqsContext.lastMessageTS = new Date().getTime();

                data.Messages.forEach(q => {
                    console.log("received message: " + JSON.stringify(q)); 
                    let roomID = ((q.MessageAttributes || {}).roomID || {}).StringValue || null;
                    try {
                        let body = JSON.parse(q.Body); 
                        body.roomID = roomID;
                        msgs.push(body);

                    } catch (parseE) {
                        msgs.push({
                            userID: 0,
                            roomID: null,
                            message: "Non-standard message: " + q.Body,
                            TS: Utils.formatDateTime(new Date())
                        });
                    
                    }
                    sqsContext.SQS.deleteMessage({
                        QueueUrl: sqsContext.mainQueueURL,
                        ReceiptHandle: q.ReceiptHandle
                    }, function (err, data) {
                        console.log("deleted message, handle=" + q.ReceiptHandle);
                    });
                });
                //msg = JSON.parse(data);
            }

            if (err || msgs.length == 0) { 
                // no message, wait and poll again
                //queueTimeout = ACTIVE_QUEUE_WAIT_MSEC; // no need to wait long
                console.log("queueListener: nomessage, err=" + err + ", runloop=" + sqsContext.runLoop);
            } else {
                // found a message - send it out
                msgs.forEach(msg => {
                    //console.log("got message row, myRow=" + Utils.squashStr(msg, 99));
                    let roomID = msg.roomID;
                    console.log("got message row, myRow=" + JSON.stringify(msg, 3));

                    if (roomID) { // re-broadcast the message
                        let room = context.rooms.find(r => r.roomID == roomID);
                        console.log("broadcast to room:");
                        console.log(room);
                        if (!room) {
                            console.log("Room not found for roomID=" + roomID + "; All rooms:");
                            console.log(context.rooms);
                        }
                        let roomUserIDs = (room || {}).userIDs || [];

                        roomUserIDs.forEach(qID => {
                            let user = context.users.find(u => u.userID == qID);
                            if (user && user.userQueueURL) {
                                // re-broadcast the message!
                                let dedupID = user.userID + ":" + roomID + ":" + (new Date().getTime());
                                sqsContext.SQS.sendMessage({ // params
                                    QueueUrl: user.userQueueURL,
                                    MessageAttributes: {
                                        userID: {DataType: "String", StringValue: "" + user.userID },
                                        roomID: {DataType: "String", StringValue: "" + qID }
                                    },
                                    MessageDeduplicationId: dedupID, // this is unique business key as a string
                                    MessageGroupId: "1", // this is the FIFO group id, may be the same
                                    MessageBody: JSON.stringify(msg)
                                }, function (err, res) {
                                    console.log("message error:" + err);
                                    //console.log("Message sent to " + user.userQueueURL + " : " + JSON.stringify(res));
                                    console.log("Message sent to " + user.userQueueURL + " : dedupID=" +  dedupID + ", body=" + JSON.stringify(msg));
                                });                
                            } else {
                                console.log("message NOT broadcast, no user " + qID + ", all users: ");
                                console.log(context.users);
                            }
                        });
                    }
                }); // end msg.forEach
                //queueTimeout = ACTIVE_QUEUE_WAIT_MSEC;
            }

            if (sqsContext.runLoop) {
                setTimeout(function() { loopSQS(sqsContext, context); }, queueTimeout);
            } else {
                //this.queueListenerRunning = false;
                console.log("loopSQS:  stopping; no messages since " + new Date(sqsContext.lastMessageTS || 0));
            }
        } catch (e1) {
            console.dir(e1);
            console.log("loopSQS failed: " + Utils.squashStr(e1));
            //setTimeout(this.queueListener, ACTIVE_QUEUE_WAIT_MSEC);
            sqsContext.runLoop = false;
        }
    });
}

/** invokes callback() with  {key, secret, userqueue}; updates context.users with the userqueue */
function createUser(sqsContext, userID, callback) {

    let iamName = config.sqsQueueRoot + "_iam_" + userID + "_" + (new Date().getTime());
    let queueName = config.sqsQueueRoot + "_user_" + userID + "_" + (new Date().getTime()) + ".fifo";

    sqsContext.SQS.createQueue({
        QueueName: queueName,
        Attributes: {
            FifoQueue: "true",
            ContentBasedDeduplication: "false"
        }
    
    }, function(err, queueData) {
        // successful response has data.QueueUrl
        if (err) {
            console.log(err, err.stack); // an error occurred
            callback(null);
        }
        else {
        console.log(queueData);           // successful response
        sqsContext.IAM.createUser({
            UserName: iamName,
            Path: "/" + config.sqsQueueRoot + "_users/"
        }, (err, data) => {
            if (err) {
                console.log("Error in createUser: " + err, err.stack); // an error occurred
                throw err;
            }
            else {
                //console.log(data);           // successful response            
                console.log("created user " + data.User.UserName + " with UserId=" + data.User.UserId);
                /*
                data = {
                User: {
                Arn: "arn:aws:iam::123456789012:user/Bob", 
                CreateDate: <Date Representation>, 
                Path: "/", 
                UserId: "AKIAIOSFODNN7EXAMPLE", 
                UserName: "Bob"
                }
                }
                */
               sqsContext.IAM.createAccessKey({
                    UserName: iamName
                }, (err, accessData) => {
                    if (err) {
                        console.log(err, err.stack); // an error occurred
                        throw err;
                    }
                    else  {   
                        console.log(accessData);           // successful response
                        console.log("created access key=" + accessData.AccessKey.AccessKeyId + ", secret=" + accessData.AccessKey.SecretAccessKey);
                        /*
                        data = {
                        AccessKey: {
                        AccessKeyId: "AKIAIOSFODNN7EXAMPLE", 
                        CreateDate: <Date Representation>, 
                        SecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY", 
                        Status: "Active", 
                        UserName: "Bob"
                        }
                        }
                        */
                       sqsContext.IAM.addUserToGroup({
                            GroupName: config.iamUserGroup,
                            UserName: iamName
                        }, (err, addGroupData) => {
                            if (err) {
                                console.log(err, err.stack); // an error occurred
                                throw err;
                            }
                            else  {   
                                console.log("addUserToGroup");
                                console.dir(addGroupData);
                                /*
                                let user = context.users.find(q => q.userID == userID);
                                if (!user) {
                                    user = {
                                        userID: userID
                                    };
                                    context.users.push(user); // shouldn't happen, really
                                }
                                user.userQueueURL = data.QueueUrl;
                                user.userQueueName = queueName;
                                */
                                callback({
                                    accessKey: accessData.AccessKey.AccessKeyId, //config.testAccessKeyId,
                                    accessSecret: accessData.AccessKey.SecretAccessKey, //config.testSecretAccessKey,
                                    queueURL: queueData.QueueUrl,
                                    queueName: queueName,
                                    userIAMName: iamName,
                                    sqsMainQueueURL: sqsContext.mainQueueURL
                                });
                            }
                        }); // end of addUserToGroup
                    }
                }); // end of createAccessKey               
            }            
        }); // end of createUser
      }   
    }); // end of createQueue callback
}

function removeUser(sqsContext, iamName, queueURL) {
    /*
    if (queueName) {
        sqs.getQueueUrl({

        }, (err, data) => {
            if (err) console.log(err, err.stack); // an error occurred
            else {
                sqs.deleteQueue({
                    QueueName: queueName
                }, function(err, data) {
                    if (err) console.log(err, err.stack); // an error occurred
                    else     console.log(data);           // successful response
                });
            }
       });
    }
    */
    if (queueURL) {
        sqsContext.SQS.deleteQueue({
            QueueUrl: queueURL
            }, function(err, data) {
                if (err) console.log(err, err.stack); // an error occurred
                else  {
                    console.log("Queue Deleted: " + queueURL);           // successful response
                    console.log(data);
                }   
        });
   } else {
       console.log("removeUser: no queueURL");
   }
}

function deleteQueueInLoop(sqs, queues, callback) {
    if (queues && queues.length > 0) {
        let url = queues.shift();
        sqs.deleteQueue({
            QueueUrl: url
        }, function(err, data) {
            if (!err) {
                console.log("deleted " + url);
                deleteQueueInLoop(sqs, queues, callback);
            }
            else {
                console.log("error in deleteQueueInLoop for " + url);
                console.dir(err);
                throw err;
            }
        });
    } else {
        callback();
    }
}

function clearQueues(sqsContext, callback) {
    sqsContext.SQS.listQueues({
        QueueNamePrefix: config.sqsQueueRoot
    }, function(err, data) {
        if (err) {
            console.log("Error in sqs_service.clear_all");
            console.dir(err);
            throw err;
        } else if (data && data.QueueUrls && data.QueueUrls.length > 0) {
            let mainQueueName = config.sqsQueueRoot + ".fifo";
            let mainInd = data.QueueUrls.findIndex(q => q.endsWith(mainQueueName));
            if (mainInd >= 0) {
                console.log("clearAll, found main queue ind " + mainInd);
                data.QueueUrls.splice(mainInd, 1);
            }
            console.log("going to delete queues, count=" + data.QueueUrls.length);
            deleteQueueInLoop(sqsContext.SQS, data.QueueUrls, callback);
        } else {
            callback();
        }
    });
    // will delete users later
}

function deleteAccessKeyInLoop(iam, accessKeys, callback) {
    if (accessKeys && accessKeys.length > 0) {
        let key = accessKeys.shift();

        iam.deleteAccessKey({
            AccessKeyId: key.AccessKeyId,
            UserName: key.UserName
        }, function(err, data) {
            if (!err) {
                console.log("deleted key for " + key.UserName);
                deleteAccessKeyInLoop(iam, accessKeys, callback);
            }
            else {
                console.log("error in deleteAccessKeyInLoop for " + userName);
                console.dir(err);
                throw err;
            }
        });
    } else {
        callback();
    }
}


function deleteUserInLoop(iam, users, callback) {
    if (users && users.length > 0) {
        let user = users.shift();
        let userName = user.UserName;

        console.log("deleteUserInLoop: trying for " + userName);
        // first, delete the access key. Loop if there is more than one
        iam.listAccessKeys({
            UserName: userName
        }, function(err, data) {
            if (err) {
                console.log("Error in sqs_service.listAccessKeys");
                console.dir(err);
                throw err;
            } 

            if (data && data.AccessKeyMetadata && data.AccessKeyMetadata.length > 0) {
                let mainUserName = config.sqsQueueRoot + "_main";
                let mainInd = data.AccessKeyMetadata.findIndex(q => q.UserName == mainUserName);
                if (mainInd >= 0) {
                    console.log("clearAccessKeys, found main user ind " + mainInd);
                    data.AccessKeyMetadata.splice(mainInd, 1);
                }
                console.log("going to delete access keys, count=" + data.AccessKeyMetadata.length)
                deleteAccessKeyInLoop(iam, data.AccessKeyMetadata, () => {
                    console.log("deleteUserInLoop for " + userName + " in callback from deleteAccessKeyInLoop");
                    users.unshift(user);
                    deleteUserInLoop(iam, users, callback); // try deleting the user again
                });
            } else {
                iam.removeUserFromGroup({
                    GroupName: config.iamUserGroup,
                    UserName: userName
                }, function(err, data) {
                    if (err) {
                        console.log("error in deleteUsersInLoop/removeUserFromGroup for " + userName);
                        console.dir(err);
                        throw err;
                    }
                    iam.deleteUser({
                        UserName: userName
                    }, function(err, data) {
                        if (!err) {
                            console.log("deleted user " + userName);
                            deleteUserInLoop(iam, users, callback);
                        }
                        else {
                            console.log("error in deleteUsersInLoop for " + userName);
                            console.dir(err);
                            throw err;
                        }
                    });
                });
            }
        });
    
    } else {
        callback();
    }
}

function clearUsers(sqsContext, callback) {
    sqsContext.IAM.listUsers({
        //PathPrefix: "/" + config.sqsQueueRoot + + "_users"
    }, function(err, data) {
        if (err) {
            console.log("Error in sqs_service.clearUsers");
            console.dir(err);
            throw err;
        } else if (data && data.Users && data.Users.length > 0) {
            let mainUserName = config.sqsQueueRoot + "_main";
            /*
            let mainInd = data.Users.findIndex(q => q == mainUserName);
            if (mainInd >= 0) {
                console.log("clearUsers, found main user ind " + mainInd);
                data.Users.splice(mainInd, 1);
            }
            */
           //console.log("clearUsers got all users: " + JSON.stringify(data.Users));
            let goodUsers = data.Users.filter(q => q.UserName.startsWith(config.sqsQueueRoot + "_") && q.UserName != mainUserName);
            console.log("going to delete users, count=" + goodUsers.length);
            deleteUserInLoop(sqsContext.IAM, goodUsers, callback);
        } else {
            callback();
        }
    });
    // will delete users later
}

// not used
function clearAccessKeys(sqsContext, callback) {
    sqsContext.IAM.listAccessKeys({
        UserName: "" + config.sqsQueueRoot + "_*"
    }, function(err, data) {
        if (err) {
            console.log("Error in sqs_service.clearAccessKeys");
            console.dir(err);
            throw err;
        } else if (data && data.AccessKeyMetadata && data.AccessKeyMetadata.length > 0) {
            let mainUserName = config.sqsQueueRoot + "_main";
            let mainInd = data.AccessKeyMetadata.findIndex(q => q.UserName == mainUserName);
            if (mainInd >= 0) {
                console.log("clearAccessKeys, found main user ind " + mainInd);
                data.AccessKeyMetadata.splice(mainInd, 1);
            }
            console.log("going to delete access keys, count=" + data.AccessKeyMetadata.length)
            deleteAccessKeyInLoop(sqsContext.IAM, data.AccessKeyMetadata, callback);
        } else {
            callback();
        }
    });
    // will delete users later
}


function clearAll(sqsContext, callback) {
    clearQueues(sqsContext, () => {
        clearUsers(sqsContext, callback);
    });
}

exports.SQSService = function(context) {

    AWS.config.update({
        accessKeyId: config.sqsAccessKeyId, //"AKIAU2XJCRU6ZHEHFRE3",
        secretAccessKey: config.sqsSecretAccessKey, //"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",
        region: config.sqsRegion //"ap-southeast-2"
    });
    
    let sqsContext = {
        SQS: new AWS.SQS({}),
        IAM: new AWS.IAM({})
    };

    // the constructor here
    // end constructor
    return {
        clearAll: function(callback) {
            console.log("clearAll called");

            clearAll(sqsContext, () => {
                console.log("clearAll finished");
                callback();
            });
        },

        start: function() {
            if (sqsContext.runLoop) {
                console.log("sqs_service.start: it is already running");
                return;
            }
            // clear all old queues, then create the main queue again and start looping
            //clearAll(sqsContext.SQS, () => {
                // get or create the main queue
                let mainQueueName = config.sqsQueueRoot + ".fifo";
                sqsContext.SQS.getQueueUrl({
                    QueueName: mainQueueName
                }, function(err, data) {
                    if (err && err.code == "AWS.SimpleQueueService.NonExistentQueue") { // queue not found - create it again
                        console.log("yes - no queue!");
                        sqsContext.SQS.createQueue({
                            QueueName: mainQueueName,
                            Attributes: {
                                FifoQueue: "true",
                                ContentBasedDeduplication: "false"
                            }
                        }, function(err, data) {
                            if (err) {
                                console.log("Failed to create queue " + mainQueueName);
                                throw err;
                            }
                            console.log("created main queue: " + data.QueueUrl);
                            sqsContext.mainQueueURL = data.QueueUrl;
                            sqsContext.runLoop = true;
                            sqsContext.lastMessageTS = new Date().getTime();
                            loopSQS(sqsContext, context);
                        });
                    }
                    else if (err) {
                        console.log("Error in sqs_service.start for " + mainQueueName);
                        //console.dir(err);
                        //console.log(JSON.stringify(err));
                        throw err;

                    } else {
                        //callback(data.QueueUrl);
                        console.log("found queue: " + data.QueueUrl);
                        sqsContext.mainQueueURL = data.QueueUrl;
                        sqsContext.runLoop = true;
                        loopSQS(sqsContext, context);
                    }            
                }); // end getQueueUrl
            //}); // end clearAll

/*
            runLoop = true;
            loopSQS(SQS, context);
            */
        },

        stop: function() {
            sqsContext.runLoop = false;
            console.log("sqs service - stopping");
        },

        createUser: function (userID, callback) { createUser(sqsContext, userID, callback); },

        removeUser: function(iamName, queueURL) { removeUser(sqsContext, iamName, queueURL); },

        clearAll: function(callback) { clearAll(sqsContext, callback); }, 

        sendMessage: function(queueURL, msg) {
            let dedupID = queueURL + "TS:" + (new Date().getTime());
            sqsContext.SQS.sendMessage({ // params
                QueueUrl: queueURL,
                //MessageAttributes: {
                //    userID: {DataType: "String", StringValue: "" + user.userID },
                //    roomID: {DataType: "String", StringValue: "" + qID }
                //},
                MessageDeduplicationId: dedupID, // this is unique business key as a string
                MessageGroupId: "1", // this is the FIFO group id, may be the same
                MessageBody: JSON.stringify(msg)
            }, function (err, res) {
                console.log("message error:" + err);
                //console.log("Message sent to " + user.userQueueURL + " : " + JSON.stringify(res));
                console.log("Message sent to " + user.userQueueURL + " : dedupID=" +  dedupID + ", body=" + JSON.stringify(msg));
            }); 
        }
    };

}
