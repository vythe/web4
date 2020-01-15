import React from 'react';
import axios from 'axios';
import {Groups} from './Groups';

function renderBill(bill) {
    let gbState = window.redux.getState();
/*
{
    "ID":3,
    "description":"Description of Test 15/01/2020 23:51:15",
    "invAffinities":[
        {"quality":"CALCULATED","toMoniker":"g1","value":1.002},
        {"quality":"CALCULATED","toMoniker":"g2","value":0.962},
        {"quality":"CALCULATED","toMoniker":"g3","value":0.966},
        {"quality":"SET","toMoniker":"g4","value":0.938}
    ],
    "publishedDate":"15/01/2020 23:51:15",
    "status":"PUBLISHED",
    "title":"Test 15/01/2020 23:51:15"
    ,profileSay: "AYE,
    , profileSayDate: "some date "
}
*/
let cells = [];
for (let g in gbState.groups) {
    let aff = bill.invAffinities.find(q => q.toMoniker == gbState.groups[g].moniker);
    let val = aff? aff.value: 0;
    cells.push (
    <td>{val}</td>
    );
}
let saySpan = "";
if (!bill.profileSay) {
    bill.sayAye = React.createRef();
    bill.sayNay = React.createRef();
    bill.sayPass = React.createRef();
    saySpan = <>
    <label><input name={bill.ID + "_say"} type="radio" ref={bill.sayAye} defaultValue="AYE"/>AYE</label>
    <label><input name={bill.ID + "_say"} type="radio" ref={bill.sayNay} defaultValue="NAY"/>NAY</label>
    <label><input name={bill.ID + "_say"} type="radio" ref={bill.sayPass} defaultValue="PASS"/>PASS</label>
    </>;
} else {
    saySpan= <span>{bill.profileSay}</span>;
}
return (<>
<tr key={bill.ID}>
    <td>{bill.ID}</td>
    <td>{bill.title}</td>
    <td>{bill.publishedDate}</td>
    {cells}
    <td>{saySpan}</td>
    <td>{bill.profileSayDate}</td>
</tr>
<tr key={bill.iD + "_descr"}>
    <td colSpan={gbState.groups.length + 4}>{bill.description}</td>
</tr>
</>)
}

export class Bills extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            gbTimestamp: null,
            bills: null
        };


        this.load = this.load.bind(this);
        this.readContext = this.readContext.bind(this);
        this.testBillClick = this.testBillClick.bind(this);

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

    testBillClick() {

        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "test_bill", 
            timeout: 400000,    // 4 seconds timeout
            params: {} 
        })
        .then (res => {
            //this.setState({bills: res.data});
            this.load();
        });
    }

    voteClick() {

    }
    load() {
    
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_bills", 
            timeout: 400000,    // 4 seconds timeout
            params: {} 
        })
        .then (res => {
            this.setState({bills: res.data});
        });
    }
    componentDidMount() {
        Groups.loadGroups((res) => {
            //this.data = res;
            //this.setState({loadCount: this.state.loadCount + 1});
            this.load();
          });
        
    }

    render() {
        let gbState = window.redux.getState();
        let bills = this.state.bills;
        let headerCells = [];
        if (!bills || !gbState.groups) {
            return (
                <div> className="pageBand" Loading..., gbState = {JSON.stringify(gbState)}</div>
            );
        } else {
            for (let g in gbState.groups) {
                headerCells.push(<th>{gbState.groups[g].name}</th>);
            }
            return (
                <div className="pageBand">
                    <br/>
                    <table>
                        <thead>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Published</th>
                            {headerCells}
                        </thead>
                        <tbody>
                            {bills.map(q => renderBill(q))}
                        </tbody>
                    </table>
                    <div style={{textAlign: 'center'}}>
                    <button onClick={this.testBillClick}>Add Test Bill</button>
                    </div>
                </div>
            );
        }
    }
}