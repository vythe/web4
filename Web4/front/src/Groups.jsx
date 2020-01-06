import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'

export class Groups extends React.Component {


  constructor(props) {
    super(props);

    // declarations: internal properties
    this.data = null; //{};
    // declarations: state
    this.state = {
      loadCount: 0
    };
    // declarations: member functions
    this.load = this.load.bind(this);
  }

  async load() {
    var app = this;
    console.log("Groups.load called");
    /*
    fetch(window.appconfig.apiurl + "get_groups")
    .then(function(res) { return res.json(); })
    .then((res) => {
      app.data = res;
      this.data = res;
      console.log("received: " + JSON.stringify(res));
      this.setState({loadCount: this.state.loadCount + 1});
    });
    */
    /*
    let res1 = await fetch(window.appconfig.apiurl + "get_groups");
    console.log("got res1");
    let res2 = await res1.json();
    console.log("got res2: " + JSON.stringify(res2));
    app.data = res2;
    this.data = res2;
    this.setState({loadCount: this.state.loadCount + 1});
    */
    axios({
  method: 'get',
  url: window.appconfig.apiurl + "get_groups"
  //, timeout: 4000,    // 4 seconds timeout
  //, data: {firstName: 'David',lastName: 'Pollock'}
})
.then(res => {
/* handle the response */
app.data = res;
this.data = res;
console.log("axios received: " + JSON.stringify(res));
this.setState({loadCount: this.state.loadCount + 1});
})
.catch(error => console.error('axios error: ' + error))
  }

  componentDidMount() {
    this.load();
    console.log("didMount final: " + JSON.stringify(this.data));
  }

  render() {
    console.log("render start: " + JSON.stringify(this.data));
    if (!this.data) {
      return (
        <div className="notification">No data</div>
      )
    } else {
      return (
        <div className="notification">{typeof(this.data) + ": " +
        //JSON.stringify(Utils.squash(this.data))
        JSON.stringify(this.data)
      }</div>
      )
    }
  }
}

/*
from:
full fetch sample:
// fetch()

const url = 'http://localhost/test.htm';
const options = {
  method: 'POST',
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json;charset=UTF-8'
  },
  body: JSON.stringify({
    a: 10,
    b: 20
  })
};

fetch(url, options)
  .then(response => {
    console.log(response.status);
    return response.json()l
  })
  .then(data => {
    console.log(data)
  })
  .catch(error => console.error(error))
  ;

full axios sample:
axios({
  method: 'post',
  url: '/login',
  timeout: 4000,    // 4 seconds timeout
  data: {
    firstName: 'David',
    lastName: 'Pollock'
  }
})
.then(response => {// handle the response
})
.catch(error => console.error('timeout exceeded'))

*/
