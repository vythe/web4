import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';
import {Room} from './Room';
import {RoomList} from './RoomList';

export class Main extends React.Component {

/* formatted message row record: {
    ID, //messageID
    title, // user name
    userID, 
    ts: // message timestamp
}
*/
    constructor(props) {
        super(props);

        this.state = {
            //users: [],
            usersTS: null, // a timestamp on appState.users to keep track of changes
            userID: null, // current user
            roomID: null // current room
        };
//this.myIsMounted = false;
        this.readContext = this.readContext.bind(this);
        this.loadSession = this.loadSession.bind(this);
        //this.onSubmitInput = this.onSubmitInput.bind(this);
        this.onLoginClick = this.onLoginClick.bind(this);
        this.onLogoutClick = this.onLogoutClick.bind(this);
        window.redux.subscribe(this.readContext);

    }

    componentDidMount() {
        //console.log("Main.componentDidMount starts");
        this.myIsMounted = true;
        //this.readContext(true);
        this.loadSession();
    }
    
    shouldComponentUpdate(nextProps, nextState) {
        if (this.state.userID == nextState.userID && this.state.roomID == nextState.roomID && this.state.usersTS >= nextState.usersTS) {
            console.log("main.shouldComponentUpdate: FALSE next state" + JSON.stringify(nextState));
            return false;
        }
        //console.log("main.shouldComponentUpdate: old state" + JSON.stringify(this.state));
        //console.log("main.shouldComponentUpdate: TRUE next state" + JSON.stringify(nextState));

        return true;
    }

    readContext(fromDidMount) {
        if (!this.myIsMounted) {
            return;
        }
        let mainState = window.redux.getState();
        let newState = {
            //users: Utils.squash(mainState.users, 99),
            //rows: [],
            usersTS: mainState.usersTS || null,
            userID: mainState.userID || null,
            roomID: mainState.roomID || null,
            subscriptionID: null,
            userName: null
        };

        if ((this.state.userID || null) != (mainState.userID || null)
            || (this.state.roomID || null) != (mainState.roomID || null)
            || this.state.usersTS != newState.usersTS) {
        //console.log("main readContext, found old roomID=" + this.state.roomID + ", new roomID=" + mainState.roomID);
//console.log("Main users found context: " + JSON.stringify(mainState.users));
//console.log("Main users found new: " + JSON.stringify(newState.users));
        // convert messages into the display format
        //if (fromDidMount) {
        //   this.state = newState;
        //} else {
            this.setState(newState);
        //}
        } else {
            //console.log("Main readContext - skip update. old userid=" + this.state.userID + ", new userID=" + mainState.userID);
        }
    }

    loadSession() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/getsession", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            //console.log("got session: " + JSON.stringify(res.data));
            this.setState({
                userID: res.data.userID || null,
                roomID: res.data.roomID || null,
                subscriptionID: res.data.subscriptionID,
                userName: res.data.userName
            }, () => {
                console.log("main loadSession updated state: " + JSON.stringify(this.state));
            });

            if (res.data.userID) {
                window.redux.dispatch({
                    type: "USER",
                    payload: {
                        userID: res.data.userID,
                        userName: res.data.userName,
                        roomID: res.data.roomID,
                        accessKey: res.data.accessKey,
                        accessSecret: res.data.accessSecret,
                        accessRegion: res.data.accessRegion,
                        userQueueURL: res.data.userQueueURL,
                        chatQueueURL: res.data.chatQueueURL
                    }
                });    
            }
            //this.refs.loginField.value = "";
            /*
            //this.setState({bills: res.data});
            bill.ID = res.data.ID;
            bill.title = res.data.title;
            bill.description = res.data.description;
            bill.status = res.data.status;
            bill.invAffinities = res.data.invAffinities;
            //alert("updated: " + JSON.stringify(bill));
            this.setState({gbTimestamp: new Date()});

            if (action == "save") {
                this.props.onSave(bill);
            }
            */
        })
        .catch((err) => {
            console.dir(err);
            alert("got loadSession catch: " + Utils.squashStr(err, 99));
        });

    }

    /*
    onSubmitInput() {
        let txt = (this.refs.inputField.value || "").trim();

        if (txt) {
            window.redux.dispatch({
                type: "INPUT",
                payload: {
                  content: txt,
                  userID: this.state.userID
                }
            });
            this.refs.inputField.value = "";
        }
    }
    */

    onLoginClick() {
        let txt = (this.refs.loginField.value || "").trim();
        let mythis = this;
        
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/login", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {login: txt} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            //alert("got login: " + JSON.stringify(res.data));
            if (res.data.userID) {
                mythis.setState({
                    userID: res.data.userID,
                    roomID: null
                });
                window.redux.dispatch({
                    type: "USER",
                    payload: {
                        userID: res.data.userID,
                        userName: res.data.userName,
                        accessKey: res.data.accessKey,
                        accessSecret: res.data.accessSecret,
                        accessRegion: res.data.accessRegion,
                        userQueueURL: res.data.userQueueURL,
                        chatQueueURL: res.data.chatQueueURL
                    }
                });
    
            }
            this.refs.loginField.value = "";
        })
        .catch((err) => {
            console.log("login catch:");
            console.dir(err);
            alert("got login catch: " + Utils.squashStr(err, 99));
        });
    }

    onLogoutClick() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/logout", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            this.setState({
                userID: null,
                roomID: null
            });
            window.redux.dispatch({
                type: "USER",
                payload: {
                  userID: null,
                  userName: null
                }
            });
    
            //this.refs.loginField.value = "";
        })
        .catch((err) => {
            console.log("logout catch:");
            console.dir(err);
            alert("got logout catch: " + Utils.squashStr(err, 99));
        });
    }
    renderRow(row) {
        return(<div key={row.ID} data_userid={row.userID} style={{display: "flex", whiteSpace: "nowrap"}}>
            <span style={{width: "200px"}}>{row.ID + ": " + row.ts}</span>
            <span style={{width: "100px"}}>{row.title}</span>
            <span style={{flexGrow: "1", whiteSpace: "normal"}}>{row.content}</span>
        </div>)
    }

    render() {
        let appState = window.redux.getState();
        //let myUser = appState.users.find(q => q.userID == (this.state.userID || -3));
        let myUser = appState.users.find(q => q.userID == (appState.userID || -3));
        //console.log("main render, state.userID=" + this.state.userID + ", appstate users=" + Utils.squashStr(appState.users, 2)) ;
        //console.log("main render, roomID=" + this.state.roomID + ", state=" + JSON.stringify(this.state));
        return(
        <div style={{display: "flex", flexDirection: "column", height: "500px"}}>
            <div className="wideMessage">{myUser? (<>
                Hello, {myUser.userName} (#{myUser.userID})
                <button onClick={this.onLogoutClick}>Change User</button>
                <input ref="loginField" type="hidden" value=""/>
                Your room is #{this.state.roomID}
            </>): (<>
                Please Log in:  {/*Utils.squashStr(this.state.users, 99) + " / " + JSON.stringify(this.state.users)*/}
                <input ref="loginField" type="text" 
                style={{width: "10em"}} 
                onKeyUp={ (evt) => {if (evt.keyCode == 13) this.onLoginClick(); }}
                />
                <button onClick={this.onLoginClick}>Go!</button>
            </>)}</div>
            {(this.state.roomID && appState.userID)? <Room roomid={this.state.roomID}/> : <RoomList/>}
        </div>);
    }
}