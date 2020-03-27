import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';
import {Groups} from './Groups';
import {ModalBox} from './ModalBox';
import {BillEdit} from './BillEdit';

/**
 * The list of all bills for editing. No voting, but more details and edit buttons.
 * Note that it reads its own copy of bills, different from the redux storage.
 */
export class BillsAdmin extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            gbTimestamp: null,
            bills: null
        };

        // properties
        this.radios = [];

        // methods
        this.load = this.load.bind(this);
        this.readContext = this.readContext.bind(this);
        this.testBillClick = this.testBillClick.bind(this);

        this.renderBill = this.renderBill.bind(this);
        this.clickReloadBills = this.clickReloadBills.bind(this);
        this.reloadBills = this.reloadBills.bind(this);
        this.clickEditBill = this.clickEditBill.bind(this);

        this.modalEditBill = React.createRef();
        this.editBill = {};

        if(window.redux) {
            window.redux.subscribe(this.readContext);
        }
    }

    readContext() {
        let gbState = window.redux.getState();

        if (this.state.gbTimestamp == null || this.state.gbTimestamp < gbState.gbTimestamp) {
            this.setState({gbTimestamp: gbState.gbTimestamp});
            this.load();
        }        
    }
    
    clickReloadBills() {
        this.reloadBills("Y");
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

    reloadBills(forceArg) {
    
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_bills_archive", 
            timeout: 400000,    // 4 seconds timeout
            params: {mode: "all", force: forceArg} 
        })
        .then (res => {
            this.setState({bills: res.data});
        });
    }

    load() {
        this.reloadBills();
    }

    componentDidMount() {
        let gbState = window.redux.getState();
        if (!gbState || !gbState.groups) {
        Groups.loadGroups((res) => {
            //this.data = res;
            //this.setState({loadCount: this.state.loadCount + 1});
            this.load();
          });        
        }
    }

 
    clickEditBill(billID) {
        /* we can use the previously loaded bills
        this.editBill = this.state.bills.find(q =>q.ID == billID);
        this.setState({editBill: this.editBill});        
        this.modalEditBill.current.setState({show: true, myval2: "test"});
        */
        // or we can load fresh
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_bill", 
            timeout: 400000,    // 4 seconds timeout
            params: {billID: billID} 
        })
        .then (res => {
            this.setState({editBill: res.data});        
            this.modalEditBill.current.setState({show: true});
         });

    }

    clickDeleteBill(billID) {
        this.setState({
            simpleConfirmText: "Delete the bill " + billID + "?",
            onSimpleConfirm: () => {
                alert("confirm delete " + billID + " clicked!");
            }
        });
        this.refs.simpleConfirm.setState({show: true})
    }
    
    clickPublishBill(billID) {
        this.setState({
            simpleConfirmText: "Publish the bill " + billID + "?",
            onSimpleConfirm: () => {
                alert("confirm publishing " + billID + " clicked!");
            }
        });
        this.refs.simpleConfirm.setState({show: true})
    }
    
    // Bill would be happier as a separate component,
    // but just for fun, we'll make it a subroutine
    renderBill(bill) {
        let gbState = window.redux.getState();
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
            if (aff) {
                let val = aff.value;
                cells.push (
                    <td key={gbState.groups[g].moniker} style={{backgroundColor: Utils.colourApproval(val), color: 'white'}}>{val}</td>
                );
            } else {
                cells.push (
                    <td key={gbState.groups[g].moniker} style={{backgroundColor: 'pink'}}>({gbState.groups[g].moniker})</td>
                );        
            }
        }

        return (<>
        <tr id={bill.ID}>
            <td>{bill.ID}</td>
            <td>{bill.title}</td>
            <td>{bill.publishedDate}</td>
            {cells}
            <td>{/*bill.status == "NEW"? (<button onClick={() => this.clickEditBill(bill.ID)}>Edit</button>) : (<span></span>)*/}
            {Utils.inList("edit", bill.actions)? (<button onClick={() => this.clickEditBill(bill.ID)}>Edit</button>) : (<span></span>)}
            {Utils.inList("delete", bill.actions)? (<button onClick={() => this.clickDeleteBill(bill.ID)}>Delete</button>) : (<span></span>)}
            {Utils.inList("publish", bill.actions)? (<button onClick={() => this.clickPublishBill(bill.ID)}>Publish</button>) : (<span></span>)}
            </td>
        </tr>
        <tr id={bill.iD + "_descr"}>
            <td></td>
            <td>{bill.status}</td>
            <td colSpan={gbState.groups.length + 3}>{bill.description}</td>
        </tr>
        </>);        
    }

    render() {
        let gbState = window.redux.getState();
        let bills = this.state.bills;
        let headerCells = [];
        if (!bills || !gbState.groups) {
            return (
                <div className="pageBand">Loading Bills... gbState = {JSON.stringify(gbState)}</div>
            );
        } else {
            for (let g in gbState.groups) {
                headerCells.push(<th key={gbState.groups[g].name}>{gbState.groups[g].name}</th>);
            }
            return (
                <div className="pageBand">
                    <br/>
                    <h3>List of Bills
                        <button onClick={this.clickReloadBills}>Reload Bills</button>
                    </h3>
                    <table>
                        <thead><tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Published</th>
                            {headerCells}
                            </tr>
                        </thead>
                        <tbody>
                            {bills.map(q => this.renderBill(q))}
                        </tbody>
                    </table>
                    <div style={{textAlign: 'center'}}>
                    <button onClick={() => {this.clickEditBill(null) ;}}>New Bill</button>
                    <button onClick={this.testBillClick}>Add Test Bill</button>
                    {/*<br/>
                    <button onClick={this.testProfile}>Test PRofile</button>
                    */}
                    </div>
                    <ModalBox ref={this.modalEditBill} closeOnVeil={true}>
                        <BillEdit bill={this.state.editBill}>                            
                        </BillEdit>
                        {(this.modalEditBill && this.modalEditBill.current != null) ?  
                        (
                            <div>myval: {Utils.squashStr(this.state.editBill)}</div>
                        ):(
                            <div>no modalEditBill</div>
                        )
                        }
                    </ModalBox>
                    <ModalBox ref="simpleConfirm" closeOnVeil={true} onConfirm={this.state.onSimpleConfirm}>
                        <div>{this.state.simpleConfirmText}</div>
                    </ModalBox>
                </div>
            );
        }
    }
}