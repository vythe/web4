import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';

export class Room extends React.Component {

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
    static ACTIVE_QUEUE_WAIT_MSEC = 100;
    static IDLE_QUEUE_WAIT_MSEC = 1000;

    // props: roomid, userid
    constructor(props) {
        super(props);

        this.state = {
            roomid: props.roomid,
            room: null, // additional room info to display
            rows: [] // chat messages
        };
        this.queueListenerRunning = false;

        this.readContext = this.readContext.bind(this);
        this.loadRoom = this.loadRoom.bind(this);
        this.queueListener = this.queueListener.bind(this);
        this.listenQueue = this.listenQueue.bind(this);

        this.onSubmitInput = this.onSubmitInput.bind(this);

        window.redux.subscribe(this.readContext);
    }

    componentDidMount() {
        this.readContext();

        if (this.state.roomid) {
            this.loadRoom(this.state.roomid);
        } else {
            this.setState({
                roomid: null,
                room: null,
                rows: []
            });
        }
    }
    
    static getDerivedStateFromProps(props, state) {
        return  {
          roomid: props.roomid
        };
      }
    
    
    readContext() {

        let appState = window.redux.getState();
        let newState = {
            users: Utils.squash(appState.users, 99),
            //rows: [],
            userID: appState.userID,
            roomID: appState.roomID,
            subscriptionID: appState.subscriptionID
        };

        this.setState(newState);
        if (newState.subscriptionID) {
            this.listenQueue();
        }
    }

    // loads room record; used to refresh room information.
    loadRoom(roomID) {
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
            };
            this.setState(newState);            
            // send the user list upstairs, it will be merged with the existing appState
            window.redux.dispatch({
                type: "USERS",
                payload: res.data.users
            });
        })
        .catch((err) => {
            console.dir(err);
            alert("got loadRoom catch: " + Utils.squashStr(err, 99));
        });
    }

    listenQueue() {
        if (!this.state.subscriptionID || this.queueListenerRunning) {
            //console.log("listenQueue skip, isRunnig:" + this.queueListenerRunning);
            return;
        }
        this.queueListenerRunning = true;
        setTimeout(this.queueListener, 1);
    }

    queueListener() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/mqpull", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {subscriptionID: this.state.subscriptionID} //JSON.stringify(data)
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            try {
                let id = 0;
                let rows = Utils.squash(this.state.rows, 99);
                rows.forEach(q => { if (q.ID > id) id = q.ID});
                let myRow = null;
                let queueTimeout;
                if (!res.data) { 
                    // subscription broken, add a message and stop polling
                    console.log("queueListener: no subscription");
                    myRow = {
                        ID : id + 1,
                        content: "[[Subscription broken: " + this.state.subscriptionID  + "]]",
                        ts: Utils.formatDateTime(new Date())
                    };
                    queueTimeout = 0;
                } else if (!res.data.message) { 
                    // no message, wait and poll again
                    myRow = null;
                    queueTimeout = Room.IDLE_QUEUE_WAIT_MSEC;
                    //console.log("queueListener: nomessage");
                } else {
                    // found a message - convert it into a view row;
                    myRow = {
                        ID : id + 1,
                        content: res.data.message,
                        ts: res.data.TS // it will be already formatted
                    };
                    queueTimeout = Room.ACTIVE_QUEUE_WAIT_MSEC;
                    // get the user name from the appState
                    let context = window.redux.getState();
                    let user = context.users.find(q => q.userID == res.data.userID);
                    if (user) {
                        myRow.title = user.userName;
                    } else {
                        myRow.title = "#" + res.data.userID;
                    }                
                }

                if (myRow) {
                    console.log("got message row, myRow=" + Utils.squashStr(myRow, 99));
                    rows.push(myRow);
                    this.setState({rows: rows});
                }
                if (queueTimeout > 0) {
                    setTimeout(this.queueListener, queueTimeout);
                } else {
                    this.queueListenerRunning = false;
                    console.log("queueListener: no subscription, stopping");
                }
            } catch (e1) {
                console.dir(e1);
                alert("queueListener failed: " + Utils.squashStr(e1));
                setTimeout(this.queueListener, Room.ACTIVE_QUEUE_WAIT_MSEC);
            }
        })
        .catch((err) => {
            console.dir(err);
            alert("got pull catch: " + JSON.stringify(err)); //Utils.squashStr(err, 99));
        });

    }
    onSubmitInput() {
        let txt = (this.refs.inputField.value || "").trim();
        this.refs.inputField.value = "";
        if (txt && this.state.subscriptionID) {
            axios({
                method: "POST",
                withCredentials: true,
                url: window.appconfig.apiurl + "/mqpush", 
                timeout: 400000,    // 4 seconds timeout
                headers: {
                    //'Content-Type': 'application/json'
                },
                //params: {id: roomID} //JSON.stringify(data)
                data: {
                    subscriptionID: this.state.subscriptionID,
                    text: txt
                } //JSON.stringify(data)
            }).then(res => {

            })
            .catch((err) => {
                console.dir(err);
                alert("got push catch: " + Utils.squashStr(err, 99));
            });
        }
    }

    onLeaveClick() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/getroom", 
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
        if (!this.state.room || !this.state.room.roomID || this.state.room.roomID != this.state.roomid || !this.state.users) {
            this.loadRoom(this.state.roomid); // this will refresh the room info and come back to render again

            return (<div className="widemessage">Loading...
            </div>);
        }

        //let myUser = this.state.users.find(q => q.userID == (this.state.userID || -3));
        return(
        <div style={{display: "flex", flexDirection: "column", height: "500px"}}>
            <div class="wideMessage">Room #{this.state.room.roomID} {this.state.room.roomName}, sub#{this.state.subscriptionID}</div>
            <div class="wideMessage">Rows raw: {Utils.squashStr(this.state.rows, 2)}</div>
            <div className="wideMessage"style={{flexGrow: "1"}}>
                <div style={{display: "flex", flexDirection: "row"}}>
                    <div style={{flexGrow: "1"}}>
                    {this.state.rows.map(q => this.renderRow(q))}
                    </div>
                    <div style={{width: "200px", border: "1px solid blue"}}>
                        {this.state.users.map(q => 
                        (<React.Fragment key={q.userID}><span>
                            {q.login + " (" + q.userID + ")"}
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
                <button onClick={this.listenQueue}>Force Listen</button>
            </div>
        </div>);
    }
}