let Utils = require('./utils.js').Utils;

exports.Controller = function(context) {

        return {
            /** takes subscriptionID and text, and it a POST (can be a get, too)*/
            push: function(req, res) {
                //console.log("REQ: " + Utils.squashStr(req, 2));
                let subscriptionID = (req.body || {}).subscriptionID || (req.query || {}).subscriptionID;
                let text = (req.body || {}).text || (req.query || {}).text;
                console.log("args: subscriptionID=" + subscriptionID + ", text=" + text);
                if (!subscriptionID || !req.session.userID) {
                    res.status(500).send("Subscription or userID is missing");
                    return;
                }
                let msgID = context.mymq.push(subscriptionID, {
                    message: text,
                    userID: req.session.userID,
                    TS: new Date()
                });
                res.status(200).send({
                    messageID: msgID
                });	
            },

            /** takes subscriptionID, returns null if there no queue or no pending messages*/
            pull: function(req, res) {
                let subscriptionID = req.query.subscriptionID;
                if (!subscriptionID) {
                    return null;
                }
                let msg = context.mymq.pull(subscriptionID);
                if (!msg) {
                    res.status(200).send({});	    
                } else {
                    let resData = {
                        message: msg.message,
                        userID: msg.userID,
                        TS: Utils.formatDate(msg.TS)
                    };
                    res.status(200).send(resData);	
                }
            },

            /** takes subscriptionID; returns "OK" if the subscription is alive; returns an error message if something died*/
            test: function(req, res) {
                let subscriptionID = req.query.subscriptionID;
                let resText = "OK";
                if (!subscriptionID) {
                    resText = "No subsdcription ID";
                } else if (!context.userID) {
                    resText = "No user ID";
                } else {
                    resText = context.mymq.test(subscriptionID) || "OK";
                }
                res.status(200).send(resText);	
            }
        };
};