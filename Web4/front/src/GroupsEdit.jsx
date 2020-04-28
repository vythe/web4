import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';
import {AffinityCell} from './Groups';
import { AffinityEdit } from './AffinityEdit';
import {WebUser} from './WebUser';

/* This class renders a groupset for editing. 
It should not depend on context, taking the data from the 
Maybe I'll need some access control later
*/
export class GroupsEdit extends React.Component {

    /*
    Expected props:
    "data" : the groupset
    "onSave": a callback to get the changed data.
    */
    constructor(props) {
        super(props);
        this.state = {
            data: Utils.squash(props.data, 99), // this should be a deep copy
            onSave: props.onSave
        };

        this.onCancelEdit = this.onCancelEdit.bind(this);
    }

    onCancelEdit() {
      axios({
        url: window.appconfig.apiurl + "action_group_set",
        method: 'get',
        withCredentials: true      
        //, timeout: 4000,    // 4 seconds timeout
        , params: {action: "discard"} // for GET it's called params, for POST it's called data
      })
      .then(res => {
        if (typeof(this.state.onSave) == "function") {
          this.state.onSave(null);
        }
      })
      .catch(error => console.error('axios error: ' + error));
    }

    render() {
        console.log("render start: " + JSON.stringify(this.data));
        if (!this.data) {
          return (<>
            <div className="notification">No data</div>
            <button onClick={this.onCancelEdit}>Cancel Edit</button>
            </>
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
              <AffinityCell key={monikers[m]} moniker={row.moniker} aff={aff} onClick={null}/>
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