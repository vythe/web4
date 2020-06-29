import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import {Utils} from './Utils';
import {Main} from './Main';
import {SQSClient} from './SQSClient';
import {createStore} from 'redux';
import * as mySQS from 'aws-sdk'; // this could be <script src="https://sdk.amazonaws.com/js/aws-sdk-SDK_VERSION_NUMBER.min.js"></script>
//let mySQS = require('aws-sdk');

/*
mySQS.config.update({
  accessKeyId: "AKIAU2XJCRU6ZHEHFRE3",
  secretAccessKey: "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",
  region: "ap-southeast-2"
});

let sqs1 = new mySQS.SQS({});

console.log("going to list queues");
sqs1.listQueues({}, function(err, res) {
console.log("err=" + err);
  console.log(JSON.stringify(res));
  });
console.log("sqs1=" + JSON.stringify(sqs1.sendMessage));
//
console.log("going to receive message");
sqs1.receiveMessage({
  QueueUrl: "https://sqs.ap-southeast-2.amazonaws.com/332275027261/awschatmain.fifo",
  AttributeNames: ["All"],
  MaxNumberOfMessages : 10,
  WaitTimeSeconds: 10
  
}, function (err, data) {        
  console.log("receiveMessage, err=" + err);
  console.log("receiveMessage, data=" + JSON.stringify(data));
});

*/

//import * as serviceWorker from './serviceWorker';

/*
row record: {
  messageID,
  userID,
  messageTS,
  content
}
user record: {
  userID,
  userName
}
*/
const mainReduxState = {
  rows: [], // array of row records
  users: [], // array of user records
  usersTS: null, // a timestamp on updating users
  userID: null, // the current user
  roomID: null, // the current room
  subscriptionID: null, // the current room subscription for mymq
  // data for the AWS SQS
  chatQueueURL: null, // post messages here
  userQueueURL: null, // read messages here
  //SQS: null, // the SQS instance to bind them all
  //
  messageLimit: 10 // not used
};

//console.log("mySQS: " + JSON.stringify(mySQS));
//console.dir(mySQS);

//mainReduxState.SQS = sqs1;
 
// action must be {type: "action type", payload: "whatever"}
function mainReduxReducer(state = mainReduxState, action) {

  var newState = Utils.squash(state, 99);
  //newState.SQS = state.SQS;

  console.log("reducer called for action=" + action.type + ", payload=" + Utils.squashStr(action.payload, 99));

  if (action.type == "INIT") { // a debug init call
    newState.users.push({
      userID: 0,
      userName: "[SYSTEM]"
    });
    newState.rows.push({
      ID: -1,
      userID: 0,
      messageTS: new Date(),
      content: "Welcoym to Awschat",
       SQS: null,
    });
  }
  else if (action.type == "INPUT") { // this is used with mymq only
    let newID = 0;
    state.rows.forEach(q => { if (q.ID >= newID) newID = q.ID + 1; });

    newState.rows.push({
      ID: newID,
      userID: action.payload.userID || state.userID,
      messageTS: new Date(),
      content: action.payload.content
    });        
  }
  else if (action.type == "USERS") {
            
    let contextUsers = Utils.squash(state.users, 99);
    let isUserChanged = false;
    action.payload.forEach(u => {
        let ux = contextUsers.findIndex(q => q.userID == u.userID);
        if (ux >= 0) {
          if(contextUsers[ux].userName != u.userName) {
            contextUsers[ux].userName = u.userName;
            isUserChanged = true;
          }
        } else {
          contextUsers.push(u);
          isUserChanged = true;
        }
    });
    if (isUserChanged) {
      newState.users = contextUsers;
      newState.usersTS = new Date();
    }
  }
  else if (action.type == "USER") {
    newState.userID = action.payload.userID;
    let contextUsers = Utils.squash(state.users, 99);
    let ux = contextUsers.findIndex(q => q.userID == action.payload.userID);
    let isUserChanged = false;
    if (ux >= 0 && action.payload.userName) {
        if(contextUsers[ux].userName != action.payload.userName) {
          contextUsers[ux].userName = action.payload.userName;
          isUserChanged = true;
        }
        
    } else if (ux < 0) {
      isUserChanged = true;
      contextUsers.push({
          userID: action.payload.userID,
          userName: action.payload.userName || "User #" + action.payload.userID
        });
    }
    if (isUserChanged) {
      newState.users = contextUsers;
      newState.usersTS = new Date();
    }
    if (action.payload.roomID) {
      newState.roomID = action.payload.roomID; // main - getsession will get the user and the room ids together
    } else if (isUserChanged) {
      newState.roomID =  null; // kick him out
    }

    if (action.payload.accessKey && action.payload.accessSecret){
      /*
      mySQS.config.update({
        accessKeyId: action.payload.accessKey, //"AKIAU2XJCRU6ZHEHFRE3",
        secretAccessKey: action.payload.accessSecret, //"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",
        region: action.payload.accessRegion //"ap-southeast-2"
      });
      window.SQS = new mySQS.SQS({});
      */
      window.NewSQS.connect(
        action.payload.accessKey, 
        action.payload.accessSecret, 
        action.payload.accessRegion
      );
      newState.chatQueueURL = action.payload.chatQueueURL;
      newState.userQueueURL = action.payload.userQueueURL;
      //alert("context, USER chatQueueURL=" + newState.chatQueueURL);
    } else {
      //newState.SQS = null;
      //window.SQS = null;
      window.NewSQS.stop(true);
      newState.chatQueueURL = "";
      newState.userQueueURL = "";
      console.log("USER: not enough access, payload=" + JSON.stringify(action.payload));
    }
  }
  else if (action.type == "ROOM") {
    newState.roomID = action.payload.roomID;
    newState.subscriptionID = action.payload.subscriptionID;
  }
  //console.log("redux dispatch, all users=" + Utils.squashStr(newState.users, 2));
  return newState;
}

window.SQS = null;
//window.AIM = null;
window.NewSQS = new SQSClient();

window.redux = createStore(mainReduxReducer);
//window.redux.dispatch({type: "INIT"});

/*
ReactDOM.render(
  <React.StrictMode>
    <Main />
  </React.StrictMode>,
  document.getElementById('root')
);
*/
ReactDOM.render(
    <Main />,
  document.getElementById('root')
);


// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
//serviceWorker.unregister();
