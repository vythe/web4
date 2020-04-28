import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'
import {TopMenu} from './TopMenu';
import { AffinityEdit } from './AffinityEdit';
import {WebUser} from './WebUser';
import { UserProfile } from './UserProfile';
import {BillsAdmin} from './BillsAdmin';
import {GroupsEdit} from './GroupsEdit';

export class AffinityCell extends React.Component {

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
      loadCount: 0,
      isEditMode: false,
      editSetID: null
    };
    // declarations: member functions
    this.load = this.load.bind(this);
    this.reloadClickHandle = this.reloadClickHandle.bind(this);
    this.renderGroupsView = this.renderGroupsView.bind(this);
    this.readContext = this.readContext.bind(this);
    this.startEdit = this.startEdit.bind(this);
    this.startNewSet = this.startNewSet.bind(this);
    this.finishEdit = this.finishEdit.bind(this);

    if(window.redux) {
      window.redux.subscribe(this.readContext);
    }

  }

  readContext() {
    let gbState = window.redux.getState();

    if (!gbState.groups) {
        Groups.loadGroups(null);  
    }
    else if (!this.state.bills 
        || this.state.gbTimestamp < gbState.gbTimestamp
        || this.state.gbTimestamp < gbState.gbGroupsTS
        || this.state.gbTimestamp < gbState.gbUserTS
    ) {
      this.data = gbState.groups;
      this.setState({
            //bills: gbState.bills,
            gbTimestamp: gbState.gbGroupsTS || gbState.gbTimestamp
        });
    } else {
    //    this.setState({
    //        gbTimestamp: gbState.gbBillsTS || gbState.gbTimestamp
    //    });
    }        
}


  componentDidMount() {
    this.load();
  }

  load() {
    console.log("### Groups load called");
    Groups.loadGroups((res) => {
      this.data = res;
      this.setState({loadCount: this.state.loadCount + 1});
    });

    if (!window.redux.getState().user) {
      WebUser.loadUser();
    }
  }

  static loadGroups(callback) {
    axios({
      url: window.appconfig.apiurl + "get_groups",
      method: 'get',
      withCredentials: true      
      //, timeout: 4000,    // 4 seconds timeout
      //, data: {firstName: 'David',lastName: 'Pollock'}
    })
    .then(res => {
      /* handle the response */
      //app.data = res.data;
      //console.log("axios received: " + JSON.stringify(res));
      //this.data = res.data;
      //this.setState({loadCount: this.state.loadCount + 1});
      if (typeof(callback) == "function") {
          callback(res.data);
      }
      console.log("### Groups load returned, dispatch re.sgroups");
      window.redux.dispatch({
        type: "GROUPS",
        payload: {
          groups: res.data,
          gbTimestamp : new Date()
        }
      });
    })
    .catch(error => console.error('axios error: ' + error))
  }

  reloadClickHandle() {
    this.load();
  }

  affinityClickHandle() {
    alert("clicked " + JSON.stringify(this));
  }

  startEdit(setID) {
    console.log("start edit with setID=" + setID + ", dataID=" + this.data.ID);
    axios({
      url: window.appconfig.apiurl + "edit_group_set",
      method: 'get',
      withCredentials: true      
      //, timeout: 4000,    // 4 seconds timeout
      , params: {id: (setID || this.data.ID)} // for GET it's called params, for POST it's called data
    })
    .then(res => {
      this.setState({
        isEditMode: true
      });
    })
    .catch(error => console.error('axios error: ' + error))
  }

  startNewSet() {
    axios({
      url: window.appconfig.apiurl + "action_group_set",
      method: 'get',
      withCredentials: true      
      //, timeout: 4000,    // 4 seconds timeout
      , data: {action: "newset"}
    })
    .then(res => {
      this.setState({
        isEditMode: true
      });
    })
    .catch(error => console.error('axios error: ' + error))
  }

  finishEdit(savedSet) {
    // need to handle groupset saving here... maybe
    this.setState({
      isEditMode: false
    });
  }

  renderGroupsView(groupList) {
    let groupCount = groupList.length;
    /* the data structure is [group... ]
    group: {moniker, name, [affinity...]}
    affinity: {toMoniker, quality, value}
    */

    let headerCells = [];
    let monikers = [];
    for (var k in groupList) {
      monikers.push(groupList[k].moniker);
      headerCells.push(
        <th key={"header_" + groupList[k].moniker}>{groupList[k].name}</th>
      );
    }
    
    let rows = [];
    for (var k in groupList) {
      let rowCells = [];
      let row = groupList[k];
      for (var m in monikers) {
        let aff = row.affinities.find(a => a.toMoniker == monikers[m]);
        rowCells.push(
        <AffinityCell key={monikers[m]} moniker={row.moniker} aff={aff} onClick={null}/>
        );
      }
      rows.push(
        <tr key={"row_" + row.moniker}>
          <td>{row.name}</td>
          {rowCells}
        </tr>
      );
    }
    return (
      <table className="info_table">
      <thead>
        <tr><th> </th><th colSpan={headerCells.length}>Affinity to (total of {rows.length})</th></tr>
        <tr><th> </th>{headerCells}</tr>
      </thead>
      <tbody>
      {rows}
      </tbody>
    </table>
    );
  }

  render() {
    //console.log("render start: " + JSON.stringify(this.data));
    let gbstate = window.redux.getState();

    if (!this.data || !gbstate.user) {
      return (
        <div className="notification">No data</div>
      );
    } else if (this.state.isEditMode) {
      return (<div>
        <WebUser/>
        <div>{Utils.squashStr(this.data, 3)}</div>
        <hr/>
        <h3>This will be Edit!</h3>
        <GroupsEdit onSave={this.finishEdit}/>
      </div>);
    } else {
      console.log("Groups.render, data=" + Utils.squashStr(this.data));
      let openEditTitle = gbstate.user.editingSetTitle || null;
      return (<div>
        <WebUser/>
        <hr/>
        <div className="pageband">
          <h3>{this.data.title || ("Group Set #" + this.data.ID)}</h3><br/>
          {this.data.description}
        </div>
        <br/>
        {(openEditTitle? (
        <button onClick={() => {this.setState({isEditMode: true});}}>Continue Editing {openEditTitle}</button>
        ) : (<>
          <button onClick={() => { this.startEdit(this.data.ID);}}>Start Edit</button>
          <button onClick={() => { this.startEdit(null);}}>Start New Set</button>
        </>)
        )}
        
        {this.renderGroupsView(this.data.groups)}
        <br/>
        <AffinityEdit moniker="a" name="GroupA" tomoniker="b" toname="GroupB" value="0.17"/>
        <h3>Bills</h3>
      <BillsAdmin/>
    </div>);
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
