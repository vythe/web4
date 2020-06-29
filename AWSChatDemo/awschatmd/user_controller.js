
exports.Controller = function(context) {

	return {

		// takes a login parameter, returns the user record
		login: function(req, res) {
		// req.query is the object of query string args
		// req.body is the object of request body (post) args

		// return a javascript object
			if (!req.query.login) {
				res.status(500).send({
					error: "login not found"
				});
			}
			if (!context.users) {
				context.users = [];
			}
			let userLogin = req.query.login;

			let user = context.users.find(q => q.userName == userLogin);
			if (!user) {
				var maxid = -1;
				context.users.forEach(q => { if (q.userID > maxid) maxid = q.userID;} );
				user = {
					userID: maxid + 1,
					userName: userLogin,
					//accessName: "queue_aim1",
				 	//accessKey: "AKIAU2XJCRU6ZHEHFRE3", // this will be needed in getSession
					//accessSecret: "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",
					//accessRegion: "ap-southeast-2",
					//chatQueueURL: context.chatQueueURL,
					//userQueueURL: context.testUserQueueURL
					userQueueURL: null,
					userQueueName: null,
					userIAMName:  null
				};
				context.users.push(user);
			}
			req.session.userID = user.userID;

			context.SQSService.createUser(user.userID, function(data) {
				if (!data) {
					user.userQueueURL = null;
				} else {
					req.session.userAccessKey = data.accessKey;
					req.session.userAccessSecret = data.accessSecret;
					
					user.userQueueURL = data.queueURL;
					user.userQueueName = data.queueName;
					user.userIAMName = data.userIAMName;
					user.sqsMainQueueURL = data.sqsMainQueueURL;
			
				}
				res.status(200).send({
					userID: user.userID,
					userName: user.login,
					accessKey: req.session.userAccessKey,
					accessSecret: req.session.userAccessSecret,
					accessRegion: context.accessRegion,
					userQueueURL: user.userQueueURL,
					chatQueueURL: user.sqsMainQueueURL //context.sqsMainQueueURL
				});		
			});
		},
		
		logout: function(req, res) {

			let user = context.users.find(q => q.userID == req.session.userID);			

			req.session.userID = null;
			if (user) {			
				context.SQSService.removeUser(user.userIAMName, user.userQueueURL); // no callback needed
				user.userIAMName = null;
				user.testUserQueueURL = null;
			}
			res.status(200).send({
				userID: null,
				userName: ""
			});		
		},

		getsession: function(req, res) {

			let user = context.users.find(q => q.userID == req.session.userID);			
			
			if (!user) {
				req.session.userID = null;
				res.status(200).send({
					userID: null,
					userName: null,
					accessKey: null,
					accessSecret: null,
					accessRegion: context.accessRegion,
					userQueueURL: null,
					chatQueueURL: null //context.sqsMainQueueURL
					});			
			} else {
				res.status(200).send({
					userID: user.userID,
					roomID: req.session.roomID,
					subscriptionID: req.session.subscriptionID, // this is from the mymq build, not needed now
					userName: user.login,
					accessKey: req.session.userAccessKey,
					accessSecret: req.session.userAccessSecret,
					accessRegion: context.accessRegion,
					userQueueURL: user.userQueueURL,
					chatQueueURL: user.sqsMainQueueURL //context.sqsMainQueueURL
				});		
			}
		},

		allusers: function(req, res) {
			if (!context.users) {
				context.users =[];
			}
			let resData = context.users.map( u => { 
				return {
				userID: u.userID,
				userName: u.userName
				};
			});
			res.status(200).send(resData);
		}
	};
}
