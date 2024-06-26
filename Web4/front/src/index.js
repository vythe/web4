import React from 'react';
import ReactDOM from 'react-dom';
import {createStore} from 'redux';
import './index.css';
import App from './App';
import {Groups} from "./Groups";
import * as serviceWorker from './serviceWorker';

const mainReduxState = {
    gbTimestamp: null,
    groups: null,
    user: null,
    profile: null
};

// action must be {type: "action type", payload: "whatever"}
function mainReduxReducer(state = mainReduxState, action) {

    console.log("reducer called for action=" + JSON.stringify(action));
    //var newState = Object.assign({}, state);
    var newState = {};
    for (var k in state) {
        newState[k] = state[k];
    }
    
    if (typeof(action.payload) == "object") {
        for (var k in action.payload) {
            newState[k] = action.payload[k];
        }
    }
    if (action.type == "PROFILE") { 
        newState["gbProfileTS"] = new Date();
        //console.log("reducer PROFILE set gbProfileTS=" + newState.gbProfileTS + ", profile=" + JSON.stringify(newState.profile));
    } else if (action.type == "GROUPS") {
        newState["gbGroupsTS"] = new Date();
    } else if (action.type == "BILLS") {
        newState["gbBillsTS"] = new Date();
    } else if (action.type == "USER") {
        newState["gbUserTS"] = new Date();
    }
    return newState;
}

window.redux = createStore(mainReduxReducer);

ReactDOM.render(<App />, document.getElementById('root'));

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
