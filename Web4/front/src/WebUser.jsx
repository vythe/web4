import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'
import {ModalBox} from './ModalBox'
import {DocFragment} from './DocFragment';

export class WebUser extends React.Component
{

    constructor(props)
    {
        super(props);
        this.state = {
            gbTimestamp: null
        };
        // fields
        this.loginField = React.createRef();
        this.passwordField = React.createRef();
        this.profileBox = React.createRef();
        this.listProfiles = React.createRef();

        // methods
        this.load = this.load.bind(this);
        this.readContext = this.readContext.bind(this);
        this.loginClick = this.loginClick.bind(this);
        this.initUserClick = this.initUserClick.bind(this);
        this.logoutClick = this.logoutClick.bind(this);
        this.selectProfile = this.selectProfile.bind(this);

        // extra
        if(window.redux) {
            window.redux.subscribe(this.readContext);
        }
    }

    readContext() {
        let gbState = window.redux.getState();

        if (this.state.gbTimestamp == null 
            || this.state.gbTimestamp < gbState.gbTimestamp
            || this.state.gbTimestamp < gbState.gbUserTS
            ) {
            this.setState({gbTimestamp: (gbState.gbUserTS || gbState.gbTimestamp)});
        }        
    }

    load() {
        WebUser.loadUser();
    }

    static loadUser() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_user", 
            timeout: 400000,    // 4 seconds timeout
            params: {} // no params should make it a logout
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    user: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("Server call failed: " + error);
        });
    }

    loginClick() {
        let loginData = {
            login: (this.loginField.current.value || "").trim()
        };
        if (!loginData.login) {
            alert("Login is required");
            return;
        }
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "login",
            timeout: 400000,    // 4 seconds timeout
            params: loginData
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    user: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("Login failed: " + error);
        });
    }

    initUserClick() {
        let loginData = {
            login: (this.loginField.current.value || "").trim()
        };
        if (!loginData.login) {
            alert("Login is required");
            return;
        }
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "init_user",
            timeout: 400000,    // 4 seconds timeout
            params: loginData
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    user: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("New user failed: " + error);
        });
    }

    logoutClick() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "logout", 
            timeout: 4000,    // 4 seconds timeout
            params: {} // no params should make it a logout
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    user: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("Server call failed: " + error);
        });
    }

    selectProfile() {
        let selectProf = this.listProfiles.current.value;
        let selectSet = this.refs.listSets.value;

        //alert("selected: " + selectProf);
        this.profileBox.current.doClose();
        let axiosData = {
            profileID: selectProf,
            setID: selectSet,
            force: "Y"
        };
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "load_profile",
            timeout: 400000,    // 4 seconds timeout
            params: axiosData
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "PROFILE",
                payload: {
                    profile: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("loadProfile failed: " + error);
        });        
    }

    componentDidMount() {
        let gbState = window.redux.getState();
        if (!gbState.user) {
            this.load();
        }
    }

    render() {
        let gbState = window.redux.getState();
        if (!gbState.user) {
            return (<div className="pageBand">Loading user details...</div>);
        }
        else if (!gbState.user.ID) {
            return (<>
                <div className="pageBand">
                    <h3>Login as test, with a blank password</h3>
                    <span>Hello, Guest </span>
                Login: <input type="text" ref={this.loginField} defaultValue=""/>
                Password: <input type="text" ref={this.passwordField} defaultValue=""/>
                <button onClick={this.loginClick}>Login</button>
                <button onClick={this.initUserClick}>Save as New User</button>
                <span>Domain: 
                    <select ref="listSets" defaultValue={gbState.user.currentSetID} onChange={this.selectProfile}>
                        {/*set options*/}
                        {Object.keys(gbState.user.groupSets).map(k => <option value={k} key={k}>{gbState.user.groupSets[k]}</option>)}
                        {/*<option value="">(New profile)</option>*/}
                    </select>

                </span>
                <img className="icon_link" 
                    src={Utils.localURL("/img/question.png")}
                    onClick={() => this.refs.userIntroBox.setState({show: true})}
                    title="Why Log in?"
                />
                </div>
                <ModalBox ref="userIntroBox" labelClose="Okay" title="Why Log in" closeOnVeil={true}>
                    <DocFragment hashtag="user:intro"/>
                </ModalBox>
                </>
            )
        } else {
            /*
            let profileOptions = [];
            for (var k in gbState.user.profiles) {
                // test = "sss";
                //alert("my k:" + test;
                profileOptions.push(
                <option value={k}>{gbState.user.profiles[k]}</option>
                );
            }*/

            return (
            <div className="pageBand"><span>Hello, {gbState.user.name} (#{gbState.user.ID}) </span>
            <button onClick={this.logoutClick}>Logout</button>
            <br/>
                {(gbState.user && gbState.user.profiles[gbState.user.currentProfileID])? gbState.user.profiles[gbState.user.currentProfileID] : ""}
                <button onClick={() => {this.profileBox.current.doOpen();}}>Change Profile</button>
                <span>Domain: 
                    <select ref="listSets" defaultValue={gbState.user.currentSetID} onChange={this.selectProfile}>
                        {/*set options*/}
                        {Object.keys(gbState.user.groupSets).map(k => <option key={k} value={k}>{gbState.user.groupSets[k]}</option>)}
                        {/*<option value="">(New profile)</option>*/}
                    </select>

                </span>

                <ModalBox ref={this.profileBox}>
                    <h3>Profiles:</h3>
                    <div>{JSON.stringify(gbState.user.profiles)}</div>
                    <select ref={this.listProfiles} defaultValue={gbState.user.currentProfileID}>
                        {/*profileOptions*/}
                        {Object.keys(gbState.user.profiles).map(k => <option value={k}>{gbState.user.profiles[k]}</option>)}
                        <option value="">(New profile)</option>
                    </select>
                    <br/>
                    <button onClick={this.selectProfile}>Choose</button>
                </ModalBox>
            </div>
            );
        }
    }
}