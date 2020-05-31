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
    render() {
        return(<>
        <div className="wideMessage">
            Create new room: <input ref="createRoomName" type="text"></input>
            <button onClick={this.onClickCreate}>Create</button>
        </div>
        <div>
            <table>
                <thead><tr>
                    <th>Created <button onClick={this.onClickRefresh}>Refresh</button></th>
                    <th>Room Name</th>
                    <th>Users</th>
                </tr></thead>
                <tbody>
                    {this.state.rooms.map(r => {
                        return <tr key={r.roomID}>
                            <td>{Utils.formatDateTime(r.createdTS)}</td>                            
                            <td><a href="#" onClick={() => {this.onClickJoin(r.roomID);}}>{r.roomName}</a></td>                    
                            <td>{r.userCount + " IDs: " + JSON.stringify(r.userIDs)}</td>
                        </tr>
                    })}
                </tbody>
            </table>
        </div>
        </>)
    }

}
