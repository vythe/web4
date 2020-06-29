import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';

/* room list record: {
    roomID,
    roomName,
    createdTS,
    usercount
}
*/
export class RoomList extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            rooms: []
        };

        this.loadRooms = this.loadRooms.bind(this);
        this.joinRoom = this.joinRoom.bind(this);
        this.onClickCreate = this.onClickCreate.bind(this);
        this.onClickRefresh = this.onClickRefresh.bind(this);
    }

    componentDidMount() {
        this.loadRooms();
    }
    loadRooms() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/allrooms", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {}
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            //alert("got room: " + JSON.stringify(res.data));
            let rooms = [];
            res.data.forEach(r => {
                let room = {
                    roomID : r.roomID,
                    roomName: r.roomName,
                    createdTS: r.createdTS,
                    userCount: (r.userIDs || []).length,
                    userIDs: r.userIDs
                };
                rooms.push(room);
            });
            rooms = rooms.sort((a, b) => { return a.createdTS > b.createdTS });

            let newState = {
                rooms: rooms
            };
            this.setState(newState);
        })
        .catch((err) => {
            alert("got catch: " + Utils.squashStr(err, 99));
        });
    }

    joinRoom(roomid, roomname) {
//alert("joinroom id=" + roomid + ", name=" + roomname);
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "/join", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
              params: {id: roomid, name: roomname}
              //data: {login: txt} //JSON.stringify(data)
        })
        .then (res => {
            //alert("got room: " + JSON.stringify(res.data));
            window.redux.dispatch({
                type: "ROOM",
                payload: {
                    roomID: res.data.roomID,
                    roomName: res.data.roomName,
                    subscriptionID: res.data.subscriptionID
                }
            });
            // notify the others
            if (window.SQS) {
                let appState = window.redux.getState();
                let user = appState.users.find(u => u.userID == appState.userID);
                let userName = (user || {}).userName || ("#" + appState.userID);
                let dedupID = "SYSTEM:" + appState.userID + ":" + (new Date().getTime());
                let msg = {
                    userID: -1, //appState.userID,
                    message: userName + " joined the room",
                    SYSTEMACTION: "JOIN",
                    TS: Utils.formatDateTime(new Date())
                            
                };
                window.SQS.sendMessage({ // params
                    QueueUrl: appState.chatQueueURL, 
                    MessageAttributes: {
                        userID: {DataType: "String", StringValue: "" + (-1)},
                        roomID: {DataType: "String", StringValue: "" + res.data.roomID }
                    },
                    MessageDeduplicationId: dedupID, // this is unique business key as a string
                    MessageGroupId: "1", // this is the FIFO group id, may be the same
                    MessageBody: JSON.stringify(msg)
                }, function (err, res) {
                    if (err) {
                        alert("Error sending message: " + err);
                        console.log("message error:" + err);
                    } else {
                    //console.log("Message sent to " + appState.chatQueueURL + " : " + JSON.stringify(res));
                    console.log("message sent, dedupID=" + dedupID + ", msg=" + JSON.stringify(msg));
                    }
                });
    
            }

        })
        .catch((err) => {
            alert("got joinRoom catch: " + Utils.squashStr(err, 99));
        });        
    }
    onClickRefresh() {
        this.loadRooms();
    }

    onClickCreate() {
        console.log("called onClickCreate");
        this.joinRoom(null, this.refs.createRoomName.value);
    }


    onClickJoin(roomID) {
        this.joinRoom(roomID, null);
    }

    static aboutFragment() {
        return (<div>
            <h2>AWS Chat Demo</h2>
            <p>This is an online chat website, implemented with NodeJS, React and Amazon Web Services.
            The purpose of this website is complete an exercise in using AWS features (SQS and IAM)
            from a React browser application and from NodeJS as the server.</p>
            <h3>How it works</h3>
            <p>Messages are exchanged via the Amazon queue service (AWS SQS). The client application in React 
                listens to the incoming "User" queue for the chat messages and sends content to a separate "Server" queue. 
                The server listens to the "Server" queue and re-translates the content to "User" queues for all users in that chat room.</p>
            <p>Each user has their own incoming queue, since AWS does not easily support multicasting.</p>
            <p>The server performs two distinct roles: 1) as the chat coordinator, it registers users, creates queues and maintains chat rooms;
                2) as the message dispatcher, it listens to the "Server" queue and routes its messages to the users.
            </p>
            <h3>Security</h3>
            <p>Each chat user is provided with their own access credentials (generated with AWS IAM) that enable access 
                only to this chat queues and only for as long as the user is registered. While these credentials are 
                made available to the browser, and a such can be stolen, they cannot be used for any other purpose 
                than being the chat client.
            </p>
            <h3>Does it really work?</h3>
            <p>As far as a demo application goes, it works. You will not find many features here that you would
                expect from a "production-ready" chat service. It lets you sign up to the chat, joint a room, send texts,
                see texts from other users.
            </p>
            <h3>Is AWS SQS good for this?</h3>
            <p>The short answer is - no. AWS services are distributed "on the cloud", which makes them inefficient, slow any costly.
                This demo proves that AWS SQS and IAM can be used in a practical web application, but there seem to be no advantages
                to it over a tradional web server architecture. 
            </p>

        </div>);
    }
    render() {
        return(<>
        <div className="wideMessage">
            Create new room: <input ref="createRoomName" type="text"
            onKeyUp={(evt) => { if (evt.keyCode == 13) this.onClickCreate();} }
            ></input>
            <button onClick={this.onClickCreate}>Create</button>
        </div>
        <div>
        <div style={{display: "inline-block", width: "30%", marginLeft: "50px", margiRight: "50px", boxSizing: "border-box"}}>{RoomList.aboutFragment()}</div>
            <table style={{display: "inline-block", verticalAlign: "top"}}>
                <thead>
                    <tr><th colspan="3"><h2>List of Rooms</h2></th></tr>
                    <tr>
                    <th>Created <button onClick={this.onClickRefresh}>Refresh</button></th>
                    <th>Room Name</th>
                    <th>Users</th>
                </tr></thead>
                <tbody>
                    {this.state.rooms.map(r => {
                        return <tr key={r.roomID}>
                            <td>{Utils.formatDateTime(r.createdTS)}</td>                            
                            <td><a href="" onClick={() => {this.onClickJoin(r.roomID);}}>{r.roomName}</a></td>                    
                            <td>{r.userCount + " IDs: " + JSON.stringify(r.userIDs)}</td>
                        </tr>
                    })}
                </tbody>
            </table>
        </div>
        </>)
    }

}
