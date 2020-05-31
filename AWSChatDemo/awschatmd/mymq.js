exports.MyMQ = function() {

    //const MAX_QUEUE_NAME_LENGTH = 100;
    const FLUSH_CYCLE_SECONDS = 10;
    const SUBSCRIPTION_IDLE_LIMIT = 100;

    let seqQueueID = 0;
    let seqMessageID = 0;
    let seqSubscriptionID = 0;

    /** the list of queues */
    let theQueues = [];
    let theSubscriptions = [];
/* a queue is an ordered list of messages (fifo);
normally, you peek or pool one, first message only;

a message is {
    id, // globally-sequential message id
    body, //javascript whatever
    messageTS, // a timestamp, just in case
    nextMessage
}

a queue is {
    id, // a globally-sequential queue id
    messageRoot // the message list head
}

a subscription is needed to keep track of polling the queue;
a queue without subscriptions will not retain any messages.

a subscription is {
    id, 
    queueID,
    subscriptionTS, 
    lastPollTS, // a timestamp to detect abandoned subscriptions
    lastMessageID, // last pulled message ID; note that it may fake (not from this queue)
    lastOnly // when true, the queue always skips to the last available message, no backlog
}
*/
    function getQueue(queueID) {
        let res = null;
        if (queueID > 0) {
            res = theQueues.find(q => q.id == queueID);
        } else { // a new queue with a new id
            for (let oldQ in theQueues)
            {
                if (theQueues[oldQ].id > seqQueueID)
                {
                    seqQueueID = theQueues[oldQ].id;
                }
            }
            res = {
                id: ++seqQueueID,
                messageRoot: null
            };
            theQueues.push(res);
        }
        //console.log("getQueue for " + queueID + ": " + JSON.stringify(res));
        return res;
    }

    function getSubscription(subscriptionID) {
        if (!subscriptionID) return null;
        return theSubscriptions.find(q => q.id == subscriptionID);
    }

    function subscribe(queueID, lastOnly) {
        let queue = getQueue(queueID);
        if (!queue) return null;

        let res = {
            id: ++seqSubscriptionID,
            queueID: queue.id,
            subscriptionTS: new Date(),
            lastPollTS: new Date(),
            lastMessageID: null,
            lastOnly: lastOnly
        };
        let msg = queue.messageRoot;
        while (msg && msg.nextMessage) msg = msg.nextMessage;

        if (msg) {
            res.lastMessageID = msg.id - 1;
        }
        theSubscriptions.push(res);
        //console.log("subsribe done:" + JSON.stringify(res));
        flush();        
        //console.log("subsribe after fluhs:" + JSON.stringify(res));
        return res.id;
    }

    function unsubscribe(subscriptionID) {
        if (!subscriptionID || subscriptionID < 0) return;

        let ind = theSubscriptions.findIndex(q => q.id == subscriptionID);
        if (ind < 0) return;

        theSubscriptions.splice(ind, 1);
        flush();
    }
    
    function flushNow() {
        //console.log("flushNow: " + new Date());
        let cutoffDate = new Date(new Date().getTime() - SUBSCRIPTION_IDLE_LIMIT * 1000);
        let activeQueues = [];
        let activeQueuesTS = {};
        let inactiveSubsInds = [];
        let inactiveQueuesInds = [];

        for (let k in theSubscriptions) {
            if (theSubscriptions[k].lastPollTS < cutoffDate) {
                inactiveSubsInds.push(k);
            } else {
                let queueID = theSubscriptions[k].queueID;
                activeQueues.push(queueID);
                if (!activeQueuesTS[queueID] || activeQueuesTS[queueID] > theSubscriptions[k].lastPollTS) {
                    activeQueuesTS[queueID] = theSubscriptions[k].lastPollTS;
                }
            }
        }
        inactiveSubsInds.sort((a, b) => { return b - a; });
        for (let k in inactiveSubsInds) {
            console.log("removed inactive sub " + theSubscriptions[inactiveSubsInds[k]].id);
            theSubscriptions.splice(inactiveSubsInds[k], 1); // there may be more to unsubscribe() then just removing...
        }
        //
        for (let k in theQueues) {
            let isActive = activeQueues.find(q => q == theQueues[k].id);
            if (isActive < 0) {
                inactiveQueuesInds.push(k);
            } else {
                let flushTS = activeQueuesTS[theQueues[k].id];
                if (flushTS) {
                    while (theQueues[k].messageRoot && theQueues[k].messageRoot.messageTS < flushTS) {
                        theQueues[k].messageRoot = theQueues[k].messageRoot.nextMessage;
                    }
                }
            }
        }
        //
        inactiveQueuesInds.sort((a, b) => { return b - a; });
        for (let k in inactiveQueuesInds) {
            console.log("removed inactive queue " + theQueues[inactiveQueuesInds[k]].id);
            theQueues.splice(inactiveQueuesInds[k], 1); // there may be more to unsubscribe() then just removing...
        }
    }

    let flushIsCycling = false;
    
    function flushProc() {
        flushNow();
        if (theQueues.length > 0) {
            setTimeout(flushProc, FLUSH_CYCLE_SECONDS * 1000);
        } else {
            flushIsCycling = false;
            console.log("flush stopped");
        }
    }

    function flush() {
        if (!flushIsCycling) {
            setTimeout(flushProc, 1);
        }
    }

    return {

        //MAX_QUEUE_NAME_LENGTH: MAX_QUEUE_NAME_LENGTH,
        /** gets a queueID or nothing; returns the subscription object;*/
        subscribe: subscribe,
        /** be nice and unsubscribe when leaving */
        unsubscribe: unsubscribe,

        /** get the first message in the queue without removing it*/
        poll: function(subscriptionID) {
            let sub = getSubscription(subscriptionID);
            if (!sub) return null;
            let queue = getQueue(sub.queueID);
            if (!queue) {
                unsubscribe(subscriptionID);
                return null;
            }
            let msg = queue.messageRoot;
            while (msg && msg.id <= (sub.lastMessageID || -1)) {
                msg = msg.nextMessage;
            }
            sub.lastPollTS = new Date();
            if (msg) {
                sub.lastMessageID = msg.id;
                return msg.body;
            }
            return null;
        },
        pull: function(subscriptionID) {
            // same as poll, almost
            let sub = getSubscription(subscriptionID);
            //console.log("got sub: " + JSON.stringify(sub));
            if (!sub) return null;
            let queue = getQueue(sub.queueID);
            //console.log("got queue: " + JSON.stringify(queue));
            if (!queue) {
                unsubscribe(subscriptionID);
                return null;
            }
            let msg = queue.messageRoot;
            while (msg && msg.id <= (sub.lastMessageID || -1)) {
                msg = msg.nextMessage;
            }
            sub.lastPollTS = new Date();
            if (msg) {
                sub.lastMessageID = msg.id;
                return msg.body;
            }
            return null;

        },
        skip: function(subscriptionID) {

            let sub = getSubscription(subscriptionID);
            if (!sub) return null;
            let queue = getQueue(sub.queueID);
            if (!queue) {
                unsubscribe(subscriptionID);
                return null;
            }
            let msg = queue.messageRoot;
            while (msg && msg.id <= (sub.lastMessageID || -1)) {
                msg = msg.nextMessage;
            }
            if (msg) {
                sub.lastMessageID = msg.id;
            }
            sub.lastPollTS = new Date();
        },

        skipAll: function(subscriptionID) {

            let sub = getSubscription(subscriptionID);
            if (!sub) return null;

            let queue = getQueue(sub.queueID);
            if (!queue) {
                unsubscribe(subscriptionID);
                return null;
            }
            let msg = queue.messageRoot;
            while (msg && msg.nextMessage) {
                msg = msg.nextMessage;
            }
            if (msg) {
                sub.lastMessageID = msg.id;
            }
            sub.lastPollTS = new Date();
        },
        /** return message.id */
        push: function(subscriptionID, body) {
            if (!body) return; // can't post a null object
            let sub = getSubscription(subscriptionID);
            if (!sub) return null;
            let queue = getQueue(sub.queueID);
            if (!queue) {
                unsubscribe(subscriptionID);
                return null;
            }
            let newMsg = {
                id: ++seqMessageID,
                body: body,
                nextMessage: null,
                messageTS: new Date()
            }

            let msg = queue.messageRoot;
            while (msg && msg.nextMessage) {
                msg = msg.nextMessage;
            }
            console.log("push ready, msg=" + JSON.stringify(msg));
            if (msg) {
                msg.nextMessage = newMsg;
            } else {
                queue.messageRoot = newMsg;
            }
//console.log("push done, queue=" + JSON.stringify(queue));
            return newMsg.id;
        },
        /** Normally it is not needed, as other methods will flush as needed */
        flush: flush,

        test: function(subscriptionID) {

            if (!subscriptionID) {
                return "No subscription ID";
            }

            let sub = getSubscription(subscriptionID);
            if (!sub) {
                return "Invalid subscription ID";
            }

            let queue = getQueue(sub.queueID);
            if (!queue) {
                unsubscribe(subscriptionID);
                return "Invalid queue ID";
            }

            return null;
        }
    };

}