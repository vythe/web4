import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'

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

        // methods
        this.load = this.load.bind(this);
        this.readContext = this.readContext.bind(this);
        this.loginClick = this.loginClick.bind(this);
        this.initUserClick = this.initUserClick.bind(this);
        this.logoutClick = this.logoutClick.bind(this);

        // extra
        if(window.redux) {
            window.redux.subscribe(this.readContext);
        }
    }

    readContext() {
        let gbState = window.redux.getState();

        if (this.state.gbTimestamp == null || this.state.gbTimestamp < gbState.gbTimestamp) {
            this.setState({gbTimestamp: gbState.gbTimestamp});
        }        
    }

    load() {
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
        if (!gbState.user.ID) {
            return (
                <div className="pageBand">
                    <h3>Login as test, with a blank password</h3>
                    <span>Hello, Guest </span>
                Login: <input type="text" ref={this.loginField} defaultValue=""/>
                Password: <input type="text" ref={this.passwordField} defaultValue=""/>
                <button onClick={this.loginClick}>Login</button>
                <button onClick={this.initUserClick}>Save as New User</button>
                </div>
            )
        } else {
            return (
            <div className="pageBand"><span>Hello, {gbState.user.name} (#{gbState.user.ID}) </span>
            <button onClick={this.logoutClick}>Logout</button>
            </div>
            );
        }
    }
}