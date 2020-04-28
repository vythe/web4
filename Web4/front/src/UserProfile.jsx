import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'

export class UserProfile extends React.Component
{
    constructor(props)
    {
        super(props);
        // we don't keep the profile object here, only the profile ID. The object is stored in redux
        this.state = {
            gbTimestamp: null,
            profileID : null
        };
        /*
        // fields
        this.loginField = React.createRef();
        this.passwordField = React.createRef();
        */
        // methods
        this.load = this.load.bind(this);
        this.reload = this.reload.bind(this);
        this.readContext = this.readContext.bind(this);
        this.saveProfile = this.saveProfile.bind(this);
        this.reloadProfile = this.reloadProfile.bind(this);
        /*
        this.loginClick = this.loginClick.bind(this);
        this.initUserClick = this.initUserClick.bind(this);
        this.logoutClick = this.logoutClick.bind(this);
        */
        // extra
        if(window.redux) {
            window.redux.subscribe(this.readContext);
        }
    }

    readContext() {
        let gbState = window.redux.getState();
//console.log("UserProfile.readContext. gbState.gbProfileTS=" + gbState.gbProfileTS + ", my gbTimestamp=" + this.state.gbTimestamp);
        if (!gbState.profile || !gbState.profile.ID) {
            this.setState({
                profileID: null,
                gbTimestamp: gbState.gbTimestamp
            });

        } else if (!gbState.profile 
            || gbState.profile.ID != this.state.profileID 
            || this.state.gbTimestamp < (gbState.gbProfileTS || gbState.gbTimestamp)
        ) {
            //UserProfile.loadProfile(); // current user, no force
            this.setState({
                profileID: gbState.profile.ID,
                gbTimestamp: (gbState.gbProfileTS || gbState.gbTimestamp)
            });
        } 
        /*else if (this.state.gbTimestamp == null || this.state.gbTimestamp < gbState.gbTimestamp) {
            //this.setState({gbTimestamp: gbState.gbTimestamp});
            UserProfile.loadProfile(null, "Y"); // current user, force
        } */       
    }

    static loadProfile(profileID, force, callback) {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_profile", 
            timeout: 400000,    // 4 seconds timeout
            params: {profileID: profileID, force: force} 
        })
        .then(response => {// handle the response
            if (typeof (callback) == "function") {
                callback(response.data);
            }
            window.redux.dispatch({
                type: "PROFILE",
                payload: {
                    profile: response.data,
                    gbProfileTS: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("Server call failed: " + error);
        });
    }

    load() {
        let gbState = window.redux.getState();
        if (!gbState.profile) {
            UserProfile.loadProfile(null, "Y");
        }
    }

    reload(profileID) {
        UserProfile.loadProfile(profileID, "Y")
        /*
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "load_profile", 
            timeout: 400000,    // 4 seconds timeout
            params: {
                profileID: profileID, 
                force: "Y"
            } 
        })
        .then(response => {// handle the response
            this.setState({profileID: response.data.ID});
            window.redux.dispatch({
                type: "PROFILE",
                payload: {
                    profile: response.data,
                    gbTimestamp: new Date()
                }
            });
        })
        .catch(error => {//console.error('timeout exceeded')
            alert("Server call failed: " + error);
        });
        */
    }
    

    saveProfile() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "save_profile", 
            timeout: 400000,    // 4 seconds timeout
            params: {} 
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    profile: response.data,
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
        if (gbState.profile == null || gbState.user == null || gbState.profile.ID != gbState.user.currentProfileID) {
            this.load();
        }
    }


    reloadProfile() {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "load_profile", 
            timeout: 400000,    // 4 seconds timeout
            params: {
                profileID: this.state.profileID,
                force: "Y"
            } 
        })
        .then(response => {// handle the response
            window.redux.dispatch({
                type: "USER",
                payload: {
                    profile: response.data,
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
        if (gbState.profile == null || gbState.user == null || gbState.profile.ID != gbState.user.currentProfileID) {
            this.load();
        }
    }

    render() {
        let gbState = window.redux.getState();
//console.log("UserProfile.render: gbState.profile=" + Utils.squashStr(gbState.profile, 2));
        if (gbState.profile == null) {
            return (<div className="pageBand">Loading profile...</div>);
        } else if (gbState.groups == null) {
                return (<div className="pageBand">Loading profile - groups...</div>);
        } else {
            let cells = [];
            let groups = gbState.groups.groups || gbState.groups;
            for (let g in groups) {
                
                let prof = gbState.profile.invAffinities.find(q => q.toMoniker == groups[g].moniker);
                let val = prof? prof.value: 0;
                cells.push (
                <span key={g} style={{backgroundColor: Utils.colourAffinity(val), color: 'white'}}> {groups[g].name}: {val}</span>
                );
            }
            //return (<div className="pageBand">{JSON.stringify(gbState.profile)}</div>);
            return (
            <div className="pageBand">
                {!gbState.profile.ID?
                (<h3>Visitor Profile</h3>)
                : (
                <h3>Profile [{gbState.profile.name}]<br/><small>Saved on {gbState.profile.saveDate}</small></h3>
                )}
                <b>Approval numbers: </b>
                {cells}
                {(gbState.user != null && gbState.user.ID) != null && <button onClick={this.saveProfile}>Save Profile</button>}
                <button onClick={this.reloadProfile}>Reload Profile</button>
            </div>
            );

        }
    }
}