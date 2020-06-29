import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';

export class Room extends React.PureComponent {

/* formatted message row record: {
    ID, //messageID
    title, // user name
    userID, 
    ts: // message timestamp
}

room record: {
	roomID,
	roomName,
	createTS,
	userIDs: []
}
*/
    // props: not used 
    constructor(props) {
        super(props);
        this.myTS = "" + new Date();
        this.state = {            
            roomID: props.roomid, //this will also come from appState // props.roomid,
            room: null, // additional room info to display, incluting userIDs[]
            userID: null, // this will come from appState
            //subscriptionID: null, // used with mymq, it is saved in appState
            rows: [] // chat messages
        };

        this.readContext = this.readContext.bind(this);
        this.loadRoom = this.loadRoom.bind(this);
        //this.queueListener = this.queueListener.bind(this);
        this.startQueueListener = this.startQueueListener.bind(this);
        this.addRow = this.addRow.bind(this);
        this.handleError = this.handleError.bind(this);
        this.handleMessage = this.handleMessage.bind(this);

        this.onSubmitInput = this.onSubmitInput.bind(this);
        this.onLeaveClick = this.onLeaveClick.bind(this);
        //this.reduxUnsubscribe = window.redux.subscribe(this.readContext);
        this.reduxUnsubscribe = null;
    }

    componentDidMount() {

        this.reduxUnsubscribe = window.redux.subscribe(this.readContext);
        if (!this.state.roomID) {
            if (window.SQS) {
                window.NewSQS.stop(false);
            }
        }
        else if (this.state.roomID && (this.state.room == null || this.state.room.roomID != this.state.roomID)) {
            this.loadRoom(this.state.roomID, "componentDidMount");
            this.startQueueListener();
        } else {
            this.startQueueListener();            
        }
    }
    
    componentWillUnmount() {
        console.log("Room.componentWillUnmount called for " + this.myTS);
        if (typeof (this.reduxUnsubscribe) == "function") {
            this.reduxUnsubscribe();
            console.log("Room.componentWillMount - redux unsubscribed");
        }

        window.NewSQS.stop(false); 
    }

    static getDerivedStateFromProps(props, state) {
        return  {
          roomID: props.roomid // we'll get the roomID from redux
        };
    }
    
    
    readContext() {
        let appState = window.redux.getState();

        if (this.state.userID != appState.userID || !this.state.usersTS || this.state.usersTS < appState.usersTS) {
            let newState = {
                //users: Utils.squash(appState.users, 99),
                //rows: [],
                userID: appState.userID,
                usersTS: appState.usersTS
                //, roomID: appState.roomID
                //subscriptionID: appState.subscriptionID // not used at all?
            };
            this.setState(newState, () => {
            //console.log("Room.after readContext, state=" + JSON.stringify(this.state));
            });
        }
        //console.log("Room.readContext, running=" + window.NewSQS.listening() + ", roomID=" + this.state.roomID);
    }

    // loads room record; used to refresh room information.
    loadRoom(roomID, calledFrom) {
        if (!calledFrom) {
            throw "loadRoom unexpected";
        }
        //console.log("loadRoom called from " + calledFrom);
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/getroom", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {id: roomID} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {

            let newState = {
                room: {
                    roomID: res.data.roomID,
                    roomName: res.data.roomName,
                    createdtS: res.data.createdtS,
                    userIDs: res.data.userIDs
                }
                //, roomID: res.data.roomID
            };
            this.setState(newState, () => {
                //console.log("after loadRoom, state=" + JSON.stringify(this.state));
            }); 
            // send the user list upstairs, it will be merged with the existing appState
            window.redux.dispatch({
                type: "USERS",
                payload: res.data.users
            });
            this.startQueueListener();
        })
        .catch((err) => {
            alert("got loadRoom catch: " + Utils.squashStr(err, 99));
            console.log("got loadRoom catch: ");
            console.dir(err);
        });
    }

    startQueueListener() {
        //console.log("Room.startQueueListener called for " + this.myTS);
        let appState = window.redux.getState();
        window.NewSQS.listen(
            appState.userQueueURL,
            this.handleMessage,
            this.handleError
        );
    }

    addRow(msg) {
        let rowID = 0;
        let rows = Utils.squash(this.state.rows, 99);
        let needToReload = false;
        let context = window.redux.getState();

        rows.forEach(q => { if (q.ID > rowID) rowID = q.ID; });

        let myRow = {
            ID : ++rowID,
            content: msg.message,
            ts: msg.TS // it will be already formatted
        };
        if (msg.SYSTEMACTION == "JOIN" || msg.SYSTEMACTION == "LEAVE") {
            needToReload = true;
        }
        // get the user name from the appState
        let user = context.users.find(q => q.userID == msg.userID);
        if (user) {
            myRow.title = user.userName + " (#" + msg.userID + ")";
        } else {
            myRow.title = "#" + msg.userID;
        }                
        rows.push(myRow);

        if (needToReload) {
            this.loadRoom(this.state.roomID, "received-systemaction");
        }
        //console.log("going to set state, rows=" + JSON.stringify(rows));
        this.setState({rows: rows}, () => {
            //console.log("after queueListen-2, state=" + JSON.stringify(this.state));
        });

    }

    handleError(err) {
        let msg = {
            userID: 0,
            message: JSON.stringify(err || "[No Data]"),
            TS: Utils.formatDateTime(new Date())  
        };
        this.addRow(msg);
        return false;
    }

    handleMessage(body) {
        //console.log("Room received message " + body);
        try {
            let parsedMsg = JSON.parse(body);
            //console.log("ROOM received message " + JSON.stringify(parsedMsg));
            this.addRow(parsedMsg);
        } catch (parseE) {
            this.addRow({
                userID: 0,
                message: "Non-standard message: " + body,
                TS: Utils.formatDateTime(new Date())
            });
            //console.log("received non-standard message " + q.Body);                        
        }
        return true;
    }

    onSubmitInput() {
        let txt = (this.refs.inputField.value || "").trim();
        this.refs.inputField.value = "";
        let appState = window.redux.getState();

        if (txt) {

            let msg = {
                userID: appState.userID,
                message: txt,
                TS: Utils.formatDateTime(new Date())
            };
            let dedupID = appState.userID + ":" + (new Date().getTime());
           //send(queueURL, msg, dedupID, attrs, onError) 
            window.NewSQS.send(
                appState.chatQueueURL,
                msg,
                dedupID,
                { 
                    userID: appState.userID,
                    roomID: this.state.roomID
                },
                () => {
                    if (!window.NewSQS.listening()) {
                        this.startQueueListener();
                    }
                },
                (err) => {
                    alert("Error sending message: " + err);
                    console.log("Error sending message: ");
                    console.dir(err);
                    return false;
                }
           );
        }
    }

    onLeaveClick() {
        let currentRoomID = this.state.roomID;
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/leave", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            window.redux.dispatch({
                type: "ROOM",
                payload: {roomID: null}
            });
            // notify the others
            let appState = window.redux.getState();
            let user = appState.users.find(u => u.userID == appState.userID);
            let userName = (user || {}).userName || ("#" + appState.userID);
            let dedupID = "SYSTEM:" + appState.userID + ":" + (new Date().getTime());
            let msg = {
                userID: -1, //appState.userID,
                message: userName + " left the room",
                SYSTEMACTION: "LEAVE",
                TS: Utils.formatDateTime(new Date())
                        
            };
            window.NewSQS.send(
                appState.chatQueueURL,
                msg,
                dedupID,
                { 
                    userID: -1,
                    roomID: currentRoomID
                },
                () => {
                    if (window.newSQS && window.newSWS.stop) {
                    window.newSQS.stop(false); // stop listening, be ready to continue
                    }
                }, 
                (err) => {
                    alert("Error sending Leave message: " + err);
                    console.log("Error sending Leave message: ");
                    console.dir(err);
                    return false;
                }
            );                
        })
        .catch((err) => {
            console.dir(err);
            alert("got onLeaveClick catch: " + Utils.squashStr(err, 99));
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
        //console.log("room render called, state=" + JSON.stringify(this.state));
        if (!this.state.room || !this.state.room.roomID || this.state.room.roomID != this.state.roomID) {
            //console.log("render - wait for loadRoom for " + this.state.roomID);

            return (<div className="widemessage">Loading...
            </div>);
        }
        let appState = window.redux.getState();
        let roomUsers = [];
        this.state.room.userIDs.forEach(q => {
            let user = appState.users.find(u => u.userID == q);
            if (user) {
                roomUsers.push(user);
            } else {
                roomUsers.push({
                    userID: q,
                    userName :"#" + q
                });
            }
        });
        //let myUser = this.state.users.find(q => q.userID == (this.state.userID || -3));
        let listenStatus = (window.NewSQS && window.NewSQS.listening())? "Listening" : "Idle";
        return(
        <div style={{display: "flex", flexDirection: "column", height: "500px"}}>
            <div className="wideMessage">Room #{this.state.room.roomID} {this.state.room.roomName}, Status: {listenStatus}</div>
            <div className="wideMessage"style={{flexGrow: "1"}}>
                <div style={{display: "flex", flexDirection: "row"}}>
                    <div style={{flexGrow: "1"}}>
                    {this.state.rows.map(q => this.renderRow(q))}
                    </div>
                    <div style={{width: "200px", border: "1px solid blue"}}>
                        {roomUsers.map(q => 
                        (<React.Fragment key={q.userID}><span>
                            {q.userName + " (" + q.userID + ")"}
                            </span><br/>                            
                        </React.Fragment>))}
                        <button onClick={this.onLeaveClick}>Leave Room</button>
                    </div>
                </div>
            </div>

            <div>
                <input ref="inputField" type="text" 
                    defaultValue={this.state.inputText} 
                    style={{width: "50em"}} 
                    onKeyUp={(evt) => { if (evt.keyCode == 13) this.onSubmitInput();} }
                />
                <button onClick={this.onSubmitInput}>Submit</button>
                <button onClick={this.startQueueListener}>Force Listen</button>
            </div>
        </div>);
    }
}