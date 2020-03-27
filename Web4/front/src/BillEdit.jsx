import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';

export class BillEdit extends React.Component {
    // this be an modal component to edit the provided bill object.
    // it won't listen to redux, because we want to be modal here: go in, edit the record, go out. No re-rendering.
    // it will read redux for data, obviously.
    // We'll have to call the server anyway (/update_bill) to recalculate affinities

    constructor(props) {

        super(props);
        this.state = {
            //gbState: window.redux? window.redux.getState() : null,
            //bill : props.bill, // use props directly, do not save them in state
            rows: []
        }

        this.updateBill = this.updateBill.bind(this);
        this.updateBillAff = this.updateBillAff.bind(this);
        this.onRowChange = this.onRowChange.bind(this);
        this.onSave = this.onSave.bind(this);

        /* this is the bill record:
        {
            "ID":3,
            "description":"Description of Test 15/01/2020 23:51:15",
            "invAffinities":[
                {"quality":"CALCULATED","toMoniker":"g1","value":1.002},
                {"quality":"CALCULATED","toMoniker":"g2","value":0.962},
                {"quality":"CALCULATED","toMoniker":"g3","value":0.966},
                {"quality":"SET","toMoniker":"g4","value":0.938}
            ],
            "actions": ["vote", "save"],
            "publishedDate":"15/01/2020 23:51:15",
            "status":"PUBLISHED",
            "title":"Test 15/01/2020 23:51:15"
            ,profileSay: "AYE,
            , profileSayDate: "some date "
        }
        */
      
    
        //this.renderBill = this.renderBill.bind(this);
    }

    /* UpdateBill record:
     public String title;
		public String description;
		public String action;
        public List<GetAffinity> invAffinities;
    */
    updateBill(action) {
        let bill = this.props.bill;
        let data = {
            ID: bill.ID,
            title: bill.title,
            description: bill.description,
            action: action,
            invAffinities: []

        };
        bill.invAffinities.map(q => {
            if (q.quality == "SET") {
                let dataAff = {
                    quality: "SET",
                    toMoniker: q.toMoniker,
                    value: q.value
                };
                data.invAffinities.push(dataAff);
            }
        });
        axios({
            method: 'post',
            withCredentials: true,
            url: window.appconfig.apiurl + "update_bill", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/json'
              },
            data: data //JSON.stringify(data)
        })
        .then (res => {
            //this.setState({bills: res.data});
            bill.ID = res.data.ID;
            bill.title = res.data.title;
            bill.description = res.data.description;
            bill.status = res.data.status;
            bill.invAffinities = res.data.invAffinities;
            alert("updated: " + JSON.stringify(bill));
            this.setState({gbTimestamp: new Date()});
        });
     }

    updateBillAff(moniker, value) {
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "update_bill_aff", 
            timeout: 400000,    // 4 seconds timeout
            headers: {
                //'Content-Type': 'application/x-www-form-urlencoded'
                //'Content-Type': 'application/json'
                },
            params: { moniker: moniker, value: value} // for GET, it is called params; for POST it is called data
        })
        .then (res => {
            //this.setState({bills: res.data});
            let bill = this.props.bill;
            bill.invAffinities = res.data.invAffinities;
            //alert("updated add: " + JSON.stringify(bill));
            this.setState({gbTimestamp: new Date()});
        });
    }     
    onRowChange(moniker) {
        let editRow = this.state.rows.find(q => q.moniker == moniker);
        //let row = bill.rowRef.current;
    //        alert("changed: " + Utils.squashStr(row.children));
        //alert("changedm id=" + moniker + ", val=" + this.refs["valRange" + moniker].value + ", refs=" + Utils.squashStr(this.refs)); //.getElementByClassName("valRange")[0].value));
        let aff = this.props.bill.invAffinities.find(q => q.toMoniker == moniker);
        let needFullUpdate = false;
        if (!aff) {
            alert("invalid moniker: " + moniker);
            return;
        }
        if (this.refs["valRange" + moniker]) {
            editRow.value = this.refs["valRange" + moniker].value;
            aff.value = this.refs["valRange" + moniker].value;
        }
        if (this.refs["valCheckbox" + moniker]) {
            aff.quality = this.refs["valCheckbox" + moniker].checked? "SET" : "CALCULATED";
            needFullUpdate = editRow.isActive ^ this.refs["valCheckbox" + moniker].checked;
            editRow.isActive = this.refs["valCheckbox" + moniker].checked;
            //this.setState({gbTimestamp: new Date()});
        }
        console.log("onRowChange called for moniker=" + moniker + ", isactive=" + editRow.isActive);
        if (needFullUpdate) {
            this.updateBill("");
        } else {
            this.updateBillAff(moniker, aff.value);
        }

        //alert(Utils.squashStr(this.state.rows[1]));
    }

    onSave() {
        this.props.bill.title = this.refs.billTitle.value;
        this.props.bill.description = this.refs.billDescription.value;
        //alert("to save bill: " + JSON.stringify(this.props.bill));
        this.updateBill("save");
        //() => this.updateBill("save")
    }

    renderBillRow(row) {
         return (
         <tr key={row.moniker} ref={row.rowRef}>
            <td>{row.title}</td>
            <td>
                <input ref={"valCheckbox" + row.moniker} type="checkbox" defaultChecked={row.isActive} onChange={() => this.onRowChange(row.moniker)}/>
            </td>
            <td className="thisTd" style={{backgroundColor: Utils.colourApproval(row.value), color: 'white'}}>
            {(row.isActive)? 
                <input ref={"valRange" + row.moniker} 
                    type="range" 
                    defaultValue={row.value} 
                    min="0" 
                    max="1" 
                    step="0.01" 
                    onChange={() => this.onRowChange(row.moniker)}
                    style={{backgroundColor: Utils.colourApproval(row.value)}}
                    />
                : <span>{row.value}</span>
            }
            </td>
            <td>{row.value}</td>
         </tr>
         );       
    }



     render() {

        let gbState = window.redux.getState();
        this.state.rows = [];
        if (this.props.bill  && this.props.bill.invAffinities) {

            for (let g in gbState.groups) {
                 let aff = this.props.bill.invAffinities.find(q => q.toMoniker == gbState.groups[g].moniker);
                 let row = {};
                if (!aff) {
                    row = {
                        moniker: gbState.groups[g].moniker,
                        value: 0,
                        isActive: false,
                        title: gbState.groups[g].name,
                        rowRef: React.createRef()
                    };
                } else {
                
                    row = {
                     moniker: gbState.groups[g].moniker,
                     value: aff.value,
                     isActive: (aff? aff.quality == "SET": false),
                     title: gbState.groups[g].name,
                     rowRef: React.createRef()
                    }
                }
                this.state.rows.push(row);
            }
        }
        //!this.state.gbState || !this.state.bill
         if ( !this.props.bill) {
             return (<div className="error">State not available, state={Utils.squashStr(this.state)}, props={Utils.squashStr(this.props)}</div>);
         }
         //let dt = new Date()+ "";
         return (<div style={{border: "1px solid black"}}>
Here be some bill: {JSON.stringify(Utils.squash(this.props.bill))}}<br/>
<table style={{width: "100%", padding: "1em"}}><tbody>
    <tr><td>Title</td>
    <td style={{width: "100%"}}><input ref="billTitle" defaultValue={this.props.bill.title} style={{width: "100%"}}/></td>
    </tr>
    <tr><td>Description</td>
    <td style={{width: "100%"}}><textarea ref="billDescription" style={{width: "100%"}} defaultValue={this.props.bill.description}></textarea></td>
    </tr>
</tbody></table>
<table><tbody>
    {this.state.rows.map(q => this.renderBillRow(q))}
</tbody></table>
<div><button onClick={this.onSave}>Save</button></div>
         </div>);
     }
}