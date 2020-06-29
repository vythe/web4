
let awsq = require("aws-sdk");


// root:
//accessKeyId: "AKIAJN3ZLZYETVJKLQ4Q",
//secretAccessKey: "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",

// user queue_aim1:
//accessKeyId: "AKIAU2XJCRU6ZHEHFRE3",
//secretAccessKey: "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",

awsq.config.update({
    accessKeyId: "AKIAU2XJCRU6ZHEHFRE3",
    secretAccessKey: "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",
    region: "ap-southeast-2"
});

let sqs = new awsq.SQS({});

sqs.listQueues({}, function(err, res) {
console.log(JSON.stringify(res));
});

/*
sqs.sendMessage({ // params
    QueueUrl: "https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo",
    MessageAttributes: {
        Title: {DataType: "String", StringValue: "some title" },
        Title2: {DataType: "String", StringValue: "another title"}
    },
    MessageDeduplicationId: "2", // this is unique business key as a string
    MessageGroupId: "1", // this is the FIFO group id, may be the same
    MessageBody: JSON.stringify({
        attr1: "val1",
        attr2: "val2"
    })
}, function (err, res) {
    console.log("message error:" + err);
    console.log("Message sent: " + JSON.stringify(res));
});
*/

sqs.createQueue({
    QueueName: "mytestqueue.fifo",
    Attributes: {
        FifoQueue: "true",
        ContentBasedDeduplication: "false"
    }

}, function(err, data) {
    // successful response has data.QueueUrl
  if (err) console.log(err, err.stack); // an error occurred
  else     console.log(data);           // successful response
});