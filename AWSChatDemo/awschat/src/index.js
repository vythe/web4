import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
//import App from './App';
import {Utils} from './Utils';
import {Main} from './Main';
import {createStore} from 'redux';

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
  userID: null, // the current user
  roomID: null, // the current room
  subscriptionID: null, // the current room subscription
  messageLimit: 10 // not used
};

// action must be {type: "action type", payload: "whatever"}
function mainReduxReducer(state = mainReduxState, action) {

  var newState = Utils.squash(state, 99);

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
      content: "Welcoym to Awschat"
    });
  }
  else if (action.type == "INPUT") {
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
    action.payload.forEach(u => {
        let ux = contextUsers.findIndex(q => q.userID == u.userID);
        if (ux >= 0) {
            contextUsers[ux] = u;
        } else {
          contextUsers.push(u);
        }
    });
    newState.users = contextUsers;
  }
  else if (action.type == "USER") {
    newState.userID = action.payload.userID;
    let contextUsers = Utils.squash(state.users, 99);
    let ux = contextUsers.findIndex(q => q.userID == action.payload.userID);

      if (ux >= 0 && action.payload.userName) {
        contextUsers[ux].userName = action.payload.userName;
      } else if (ux < 0) {
        contextUsers.push({
          userID: action.payload.userID,
          userName: action.payload.userName || "User #" + action.payload.userID
        });
      }
      newState.users = contextUsers;      
  }
  else if (action.type == "ROOM") {
    newState.roomID = action.payload.roomID;
    newState.subscriptionID = action.payload.subscriptionID;
    /*
    let contextUsers = Utils.squash(state.users, 99);
    let ux = contextUsers.findIndex(q => q.userID == action.payload.userID);

      if (ux >= 0 && action.payload.userName) {
        contextUsers[ux].userName = action.payload.userName;
      } else if (ux < 0) {
        contextUsers.push({
          userID: action.payload.userID,
          userName: action.payload.userName || "User #" + action.payload.userID
        });
      }
      newState.users = contextUsers;      
      */
  }
  console.log("redux dispatch, all users=" + Utils.squashStr(newState.users, 2));
  return newState;
}

window.redux = createStore(mainReduxReducer);
window.redux.dispatch({type: "INIT"});

ReactDOM.render(
  <React.StrictMode>
    <Main />
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
//serviceWorker.unregister();
