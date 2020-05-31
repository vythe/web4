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
            rows: [],
            users: [],
            userID: null, // current user
            roomID: null // current room
        };
this.myIsMounted = false;
        this.readContext = this.readContext.bind(this);
        this.loadSession = this.loadSession.bind(this);
        this.onSubmitInput = this.onSubmitInput.bind(this);
        this.onSubmitLogin = this.onSubmitLogin.bind(this);
        this.onLogoutClick = this.onLogoutClick.bind(this);
        window.redux.subscribe(this.readContext);

    }

    componentDidMount() {
        this.myIsMounted = true;
        //this.readContext(true);
        this.loadSession();
    }
    
    readContext(fromDidMount) {
        if (!this.myIsMounted) {
            return;
        }
        let mainState = window.redux.getState();
        let newState = {
            users: Utils.squash(mainState.users, 99),
            rows: [],
            userID: mainState.userID,
            roomID: mainState.roomID
        };

        console.log("main readContext, found roomID=" + mainState.roomID);
//console.log("Main users found context: " + JSON.stringify(mainState.users));
//console.log("Main users found new: " + JSON.stringify(newState.users));
        // convert messages into the display format
        for (let k in mainState.rows) {
            let stateRow = mainState.rows[k];
            console.log("Main row found:" + Utils.squashStr(stateRow, 4));
            let myRow = {
                ID : stateRow.ID,
                content: stateRow.content,
                ts: Utils.formatDateTime(stateRow.messageTS)
            };
            let user = newState.users.find(q => q.userID == stateRow.userID);
            if (user) {
                myRow.title = user.userName;
            } else {
                myRow.title = "#" + stateRow.userID;
            }
            newState.rows.push(myRow);
        }
        //if (fromDidMount) {
        //   this.state = newState;
        //} else {
            this.setState(newState);
        //}
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
            console.log("got session: " + JSON.stringify(res.data));
            this.setState({
                userID: res.data.userID,
                roomID: res.data.roomID,
                subscriptionID: res.data.subscriptionID,
                userName: res.data.login
            });

            if (res.data.userID) {
                window.redux.dispatch({
                    type: "USER",
                    payload: {
                      userID: res.data.userID,
                      userName: res.data.login
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

    onSubmitLogin() {
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
            alert("got login: " + JSON.stringify(res.data));
            if (res.data.userID) {
                mythis.setState({
                    userID: res.data.userID
                });
                window.redux.dispatch({
                    type: "USER",
                    payload: {
                      userID: res.data.userID,
                      userName: res.data.userName
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
                userID: null
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
            console.log("login catch:");
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
        let myUser = this.state.users.find(q => q.userID == (this.state.userID || -3));
        //console.log("main render, state.userID=" + this.state.userID + ", state users=" + Utils.squashStr(this.state.users, 2)) ;
        return(
        <div style={{display: "flex", flexDirection: "column", height: "500px"}}>
            <div className="wideMessage">{myUser? (<>
                Hello, {myUser.login} (#{myUser.userID})
                <button onClick={this.onLogoutClick}>Change User</button>
                <input ref="loginField" type="hidden" value=""/>
            </>): (<>
                Please Log in:  {/*Utils.squashStr(this.state.users, 99) + " / " + JSON.stringify(this.state.users)*/}
                <input ref="loginField" type="text" style={{width: "10em"}} onKeyUp={ (evt) => {if (evt.code == 13) this.onSubmitLogin(); }}/>
                <button onClick={this.onSubmitLogin}>Go!</button>
            </>)}</div>
            {/*}
            <div className="wideMessage"style={{flexGrow: "1"}}>
                <div style={{display: "flex", flexDirection: "row"}}>
                    <div style={{flexGrow: "1"}}>
                    {this.state.rows.map(q => this.renderRow(q))}
                    </div>
                    <div style={{width: "200px"}}>
                        {this.state.users.map(q => 
                        (<React.Fragment key={q.userID}><span>
                            {q.login + " (" + q.userID + ")"}
                            </span><br/>                            
                        </React.Fragment>))}
                    </div>
                </div>
            </div>
            */}
            {this.state.roomID? <Room roomid={this.state.roomID}/> 
            : <RoomList/>
            }
        </div>);
    }
}