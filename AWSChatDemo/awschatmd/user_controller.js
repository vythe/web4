
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

			let user = context.users.find(q => q.login == userLogin);
			if (!user) {
				var maxid = -1;
				context.users.map(q => { if (q.userID > maxid) maxid = q.userID;} );
				user = {
					userID: maxid + 1,
					login: userLogin
				};
				context.users.push(user);
			}
			req.session.userID = user.userID;

			res.status(200).send({
				userID: user.userID,
				userName: user.login
			});		
		},
		
		logout: function(req, res) {

			req.session.userID = null;
			
			res.status(200).send({
				userID: null,
				login: ""
			});		
		},

		getsession: function(req, res) {

			let user = context.users.find(q => q.userID == req.session.userID);			
			
			res.status(200).send({
				userID: req.session.userID,
				roomID: req.session.roomID,
				subscriptionID: req.session.subscriptionID,
				login: user? user.login : ""
			});		
		},

		allusers: function(req, res) {
			if (!context.users) {
				context.users =[];
			}
			res.status(200).send(context.users);
		}
	};
}
