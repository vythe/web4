import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'
import {TopMenu} from './TopMenu';
import { AffinityEdit } from './AffinityEdit';
import {WebUser} from './WebUser';
import { UserProfile } from './UserProfile';

class AffinityCell extends React.Component {

  constructor(props) {
    super(props);
    // props.moniker
    // props.aff = {toMoniker, quality, value}
    // props.onClick = function(moniker, toMoniker, value, quality) from the parent
    this.state = {
      aff: props.aff,
      onClick: props.onClick
    };
    this.cellClickHandler = this.cellClickHandler.bind(this);    
  }

  cellClickHandler() {
    alert(JSON.stringify({moniker: this.props.moniker, ...this.props.aff}))
  }
  render() {
    let aff = this.state.aff;

    let valColour = "inherit";
    let text = "";
    let title = "";
    if (!aff || aff.quality == "NONE") {
      text = "---";
      title = "Not defined";
    } else if (aff.quality == "CALCULATED") {
      valColour = "#20" + (aff.value * 255).toString(16).padStart(2, "0").substring(0, 2) + "20";
      text = aff.value;
      title = "Calculated";
    } else {
      valColour = "#" 
      + (aff.value * 255).toString(16).padStart(2, "0").substring(0, 2)
      + (aff.value * 255).toString(16).padStart(2, "0").substring(0, 2) 
      + "20";
      text = aff.value;
      title = "Assigned";
    }
    return (
      <td tomoniker={aff.toMoniker} title={title} style={{backgroundColor: valColour}} onClick={this.cellClickHandler}>{text}</td>
    );
    
  }

}

//========================================================================
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
    this.reloadClickHandle = this.reloadClickHandle.bind(this);
  }

  componentDidMount() {
    this.load();
  }

  async load() {
    /*
    console.log("Groups.load called");
    var app = this;
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
    //app.data = res.data;
    this.data = res.data;
    console.log("axios received: " + JSON.stringify(res));
    this.setState({loadCount: this.state.loadCount + 1});
    window.redux.dispatch({
      type: "GROUPS",
      payload: {
        groups: res.data
      }
    })
    })
    .catch(error => console.error('axios error: ' + error))
  }

  reloadClickHandle() {
    this.load();
  }

  affinityClickHandle() {
    alert("clicked " + JSON.stringify(this));
  }

  render() {
    console.log("render start: " + JSON.stringify(this.data));
    if (!this.data) {
      return (
        <div className="notification">No data</div>
      )
    } else {
      let groupCount = this.data.length;
      /* the data structure is [group... ]
      group: {moniker, name, [affinity...]}
      affinity: {toMoniker, quality, value}
      */

      let headerCells = [];
      let monikers = [];
      for (var k in this.data) {
        monikers.push(this.data[k].moniker);
        headerCells.push(
          <th key={"header_" + this.data[k].moniker}>{this.data[k].name}</th>
        );
      }
      
      let rows = [];
      for (var k in this.data) {
        let rowCells = [];
        let row = this.data[k];
        for (var m in monikers) {
          let aff = row.affinities.find(a => a.toMoniker == monikers[m]);
          rowCells.push(
          <AffinityCell moniker={row.moniker} aff={aff} onClick={null}/>
          );
        }
        //rows.push(
        //  <tr key={this.data[k].moniker}><td>{JSON.stringify(this.data[k])}</td></tr>
        //)
        rows.push((
          <tr key={"row_" + row.moniker}>
            <td>{row.name}</td>
            {rowCells}
          </tr>
        ))
      }

      return (
      <div>
{/*}        <TopMenu/> */}
        <WebUser/>
        <UserProfile/>
        {/*}
        <div className="notification">LoadCount {this.state.loadCount}
            <button onClick={this.reloadClickHandle}>Reload</button>
        </div>
        <div className="notification">{typeof(this.data) + ": " +
        //JSON.stringify(Utils.squash(this.data))
        JSON.stringify(this.data)
      }</div>
    */}
        <table className="info_table">
          <thead>
            <tr><th> </th><th colSpan={headerCells.length}>Affinity to (total of {rows.length})</th></tr>
            <tr><th> </th>{headerCells}</tr>
          </thead>
          <tbody>
          {rows}
          </tbody>
        </table>
        <br/>
        <AffinityEdit moniker="a" name="GroupA" tomoniker="b" toname="GroupB" value="0.17"/>
      </div>
      );
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
