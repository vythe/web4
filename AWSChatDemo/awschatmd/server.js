
var config = require("./config").config;
var express = require("express");
const session = require("express-session");


/*
user record: {
	userID,
	login
}

room record: {
	roomID,
	roomName,
	queueID,
	createTS,
	userIDs: []
}
*/

/* 
AWS credentials:

root user:
    AWS Account ID: 332275027261
    Access Key ID: AKIAJN3ZLZYETVJKLQ4Q
    Secret Access Key: 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

user queue_aim1:
	access key  AKIAU2XJCRU6ZHEHFRE3, 
	secret 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

test queue: awschatmain.fifo
	queue URL:  	https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo
*/

var mymq = require("./mymq.js").MyMQ();

var testSubscription = mymq.subscribe(null, null);

var context = {
	mymq: mymq,
	SQSService: null,
	users: [],
	rooms: [],
	accessRegion: "ap-southeast-2",
	//chatQueueURL: "https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo",
	testUserQueueURL: "https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo"
};

/* session props: {
	userID: null,
	roomID: null,
	subscriptionID: null
}
*/

/*
console.log("testsub: " + context.testSubscription);
let m1 = context.mymq.push(context.testSubscription, "some message-1");
console.log("m1=" + m1);
context.mymq.push(context.testSubscription, "some message-2");
context.mymq.push(context.testSubscription, "some message-3");
context.mymq.push(context.testSubscription, "some message-4");
console.log("pull-1: " + JSON.stringify(
	context.mymq.pull(context.testSubscription))
);
console.log("pull-2: " + JSON.stringify(
	context.mymq.pull(context.testSubscription))
);
console.log("pull-3: " + context.mymq.pull(context.testSubscription));
console.log("pull-4: " + context.mymq.pull(context.testSubscription));
console.log("pull-5: " + context.mymq.pull(context.testSubscription));
console.log("pull-6: " + context.mymq.pull(context.testSubscription));
console.log("pull-7: " + context.mymq.pull(context.testSubscription));
*/

context.users.push({
	userID: -1,
	login:"SYSTEM_SERVER"
});

context.users.push({
	userID: 0,
	login:"EMPTY_USER"
});

context.SQSService = require("./sqs_service").SQSService(context);

context.SQSService.clearAll(() => { 
	console.log("clearAll successful");
	context.SQSService.start();
});

var user_controller = require("./user_controller").Controller(context);
var room_controller = require("./room_controller").Controller(context);
var mq_controller = require("./mq_controller").Controller(context);

var app = express();

/* don't need it right now, but good to know; from https://expressjs.com/en/4x/api.html#req.app
app.use(express.json()) // for parsing application/json
app.use(express.urlencoded({ extended: true })) // for parsing application/x-www-form-urlencoded

req.params is an object containing properties mapped to the named route “parameters”. 
	For example, if you have the route /user/:name, then the “name” property is available as req.params.name. 
	This object defaults to {}.

const session = require('express-session');
const bodyParser = require('body-parser');
const router = express.Router();
const app = express();

app.use(session({secret: 'ssshhhhh',saveUninitialized: true,resave: true}));
app.use(bodyParser.json());      

router.post('/login',(req,res) => {
    sess = req.session; // get the session, do something there
    sess.email = req.body.email;
    res.end('done');
});
*/

app.use(session({secret: 'some',saveUninitialized: true,resave: true}));

app.use(function (req, res, next) {
	if (config.corsURL) {
		//res.header('Access-Control-Allow-Origin', '*');
		res.header('Access-Control-Allow-Origin', config.corsURL);
	}
    res.header('Access-Control-Allow-Credentials', 'true');
    res.header('Access-Control-Allow-Methods', 'GET,HEAD,PUT,PATCH,POST,DELETE');
    res.header('Access-Control-Expose-Headers', 'Content-Length');
    res.header('Access-Control-Allow-Headers', 'Accept, Authorization, Content-Type, X-Requested-With, Range');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    } else {
        return next();
    }
});

// routes
app.get("/", function(req, res) {
	res.send("Hello, world");
});

app.use(express.static('public'));
app.use(express.json()) // for parsing application/json POSTs
app.use(express.urlencoded({ extended: true })) // for parsing application/x-www-form-urlencoded


app.get("/login", user_controller.login);
app.get("/allusers", user_controller.allusers);
app.get("/logout", user_controller.logout);
app.get("/getsession", user_controller.getsession);


app.get("/join", room_controller.join);
app.get("/leave", room_controller.leave);
app.get("/allrooms", room_controller.allrooms);
app.get("/roomusers", room_controller.roomusers);
app.get("/getroom", room_controller.getroom);

app.post("/mqpush", mq_controller.push);
app.get("/mqpush_test", mq_controller.push);
app.get("/mqpull", mq_controller.pull);



// start app
app.listen(config.expressport, () => console.log(`Example app listening at http://localhost:${config.expressport}`));