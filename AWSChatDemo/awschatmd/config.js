exports.config = {
	expressport: 8080,
	corsURL: "http://localhost:3000",
	//mongodb: "mongodb://web4:vbelevweb4@vbelev.net:27017/web4?authSource=admin",
	//
	sqsAccessKeyId: "AKIAU2XJCRU6S6EWQXVX",  // awschat_main
	sqsSecretAccessKey: "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",
	sqsRegion: "ap-southeast-2",
	//sqsMainQueueURL: "https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo",
	sqsQueueRoot: "awschat",
	iamUserGroup: "queueaimgroup",
	loopIdleTimeoutSec: 1000,
	//
	// user queue_aim1 credentials
	testAccessKeyId: "AKIAU2XJCRU6ZHEHFRE3",
	testSecretAccessKey: "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",

};
