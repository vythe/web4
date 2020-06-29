import * as SQSPackage from 'aws-sdk'; // this could be <script src="https://sdk.amazonaws.com/js/aws-sdk-SDK_VERSION_NUMBER.min.js"></script>
import {Utils} from './Utils';

export class SQSClient {

    static ACTIVE_QUEUE_WAIT_MSEC = 100;
    static IDLE_QUEUE_WAIT_SEC = 30;

    constructor() {
        this.SQS = null;
        this.queue = null;
        this.isListening = false;
        this.lastMessageTS = 0;

        this.connect = this.connect.bind(this);
        this.listen = this.listen.bind(this);
        this.listening = this.listening.bind(this);
        this.stop = this.stop.bind(this);
        this.send = this.send.bind(this);

        this.reportError = this.reportError.bind(this);
        this.queueListener = this.queueListener.bind(this);
    }

    connect(accessKey, accessSecret, accessRegion) {
        SQSPackage.config.update({
            accessKeyId: accessKey, 
            secretAccessKey: accessSecret, 
            region: accessRegion
        });
        this.isListening = false;
        this.SQS = new SQSPackage.SQS({});   
    }

    listening() {
        return this.isListening;
    }

    /**
     * onMessage(msg) and onError(err) should return true to continue listening; false to stop listening;
     * if they do not return anything, an error will stop listening, a message will continue listening.
     */
    listen(queueURL, onMessage, onError) {
        this.messageCallback = onMessage;
        this.errorCallback = onError;
        if (!this.lastMessageTS) {
            this.lastMessageTS = new Date().getTime();
        }
        if (this.queue == queueURL && this.isListening) {
            // all good, carry on
            return;
        }
        this.queue = queueURL;
        this.isListening = true;
        setTimeout(this.queueListener, 10);
    }

    stop(withDisconnect) {
        this.isListening = false;
        this.lastMessageTS = 0;
        if (withDisconnect) {
            this.SQS = null;
            this.queue = null;
        }
    }

    send(queueURL, msg, dedupID, attrs, onComplete, onError) {
        if (!this.SQS) {
            if (typeof(onError) == "function") {
                onError("SQSClient not initialized");
            }
            return;
        }
        let messageAttributes = {};
        if (typeof(attrs) == "object") {
            for (let k in attrs) {
                messageAttributes[k] = {
                    DataType: "String", 
                    StringValue: "" + attrs[k]
                }
            }
        }
        this.SQS.sendMessage({ // params
            QueueUrl: queueURL, 
            MessageAttributes: messageAttributes,
            MessageDeduplicationId: dedupID, // this is unique business key as a string
            MessageGroupId: "1", // this is the FIFO group id, may be the same
            MessageBody: JSON.stringify(msg)
        }, function (err, res) {
            if (err) {
                alert("SQSClient.send: error sending message: ");
                console.dir(err);
                if (typeof(onError) == "function") {
                    onError(err);
                }
                
            } else {
                //console.log("Message sent to " + appState.chatQueueURL + " : " + JSON.stringify(res));
                //console.log("message sent, dedupID=" + dedupID + ", msg=" + JSON.stringify(msg));
                if (typeof(onComplete) == "function") {
                    onComplete();
                }
            }
        });
    }

    reportError(err) {
        if (typeof (this.errorCallback) == "function") {
            return this.errorCallback(err) || false;
        }
        return false;
    }

    reportMessage(msg) {
        if (typeof (this.messageCallback) == "function") {
            let res = this.messageCallback(msg);
            if (typeof (res) == "undefined") {
                return true;
            }
            return res;
        }

        return false;
    }

    queueListener() {
        if (!this.SQS || !this.queue) {
            this.isListening = false;
            this.reportError("SQSClient not initialized");
            return; // no connection to the queue - probably not logged in.
        }

        if (typeof(this.messageCallback) != "function") {
            this.isListening = false;
            this.reportError("SQSClient: no message callback, cannot continue");
            return;
        }

        //console.log("SQSClient.queueListener going to receiveMessage on " + this.queue + ", isListening=" + this.isListening);
        this.SQS.receiveMessage({
            QueueUrl: this.queue,
            //AttributeNames: ["All"],
            MessageAttributeNames: ["All"],
            MaxNumberOfMessages : 10,
            WaitTimeSeconds: 10
            
        }, (err, data) => {        
            try {
                //console.log("SQSClient got data: " + JSON.stringify(data));
                if (err || !data) {
                    //console.log("queueListener, error: " + JSON.stringify((err || "[No Data]")));
                    this.isListening = this.reportError(err || "[No Data]");

                } else if (!data.Messages || data.Messages.length == 0) {
                    // no messages, skip
                    if (this.lastMessageTS + SQSClient.IDLE_QUEUE_WAIT_SEC * 1000 < new Date().getTime()) {
                        this.isListening = this.reportError("Queue is idle since " + Utils.formatDateTime(new Date(this.lastMessageTS)));
                    }
                    //console.log("SQS Client: no messages at " + Utils.formatDateTime(new Date()));
                    if (!this.isListening) {
                        let msg = {
                            userID: -1, //appState.userID,
                            message: "No activity for " + Math.round((new Date().getTime() - this.lastMessageTS ) / 1000.) + " seconds, stopping",
                            SYSTEMACTION: "IDLE",
                            TS: Utils.formatDateTime(new Date())                                    
                        };
                        this.reportMessage(JSON.stringify(msg));
                    }
                } else {
                    this.lastMessageTS = new Date().getTime();
                    data.Messages.forEach(q => {
                        if (this.isListening) {
                            console.log("received message: " + q.Body); // JSON.stringify(q)); 
                            this.isListening = this.reportMessage(q.Body);

                            this.SQS.deleteMessage({
                                QueueUrl: this.queue,
                                ReceiptHandle: q.ReceiptHandle
                            }, function (err, data) {
                                //console.log("deleted message, handle=" + q.ReceiptHandle);
                                if (err) {
                                    this.isListening = this.reportError(err);
                                }
                            });
                        }
                    });
                }

                if (this.isListening) {
                    setTimeout(this.queueListener, SQSClient.ACTIVE_QUEUE_WAIT_MSEC);
                } else {
                    this.isListening = false;
                    //console.log("SQSClient.queueListener: turned off, stopping: isListening=" + this.isListening);    
                }
            } catch (e1) {
                console.dir(e1);
                //alert("queueListener failed: " + Utils.squashStr(e1));
                if (this.reportError(e1)) {
                    setTimeout(this.queueListener, SQSClient.ACTIVE_QUEUE_WAIT_MSEC);
                }
            }            
        });
    }
}