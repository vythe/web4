import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';
import {Groups} from './Groups';


export class Bills extends React.Component {

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
        this.testProfile = this.testProfile.bind(this);

        this.renderBill = this.renderBill.bind(this);
        this.clickVote = this.clickVote.bind(this);
        this.clickReloadBills = this.clickReloadBills.bind(this);
        this.reloadBills = this.reloadBills.bind(this);

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

    clickVote(e1) {
        let billid = e1.target.getAttribute('billid') 
        let mybill = this.state.bills.find(q => q.ID == billid);
        //console.log(JSON.stringify(Utils.squash(this.radios[0].current)));
        //alert("mydata:" + this.radios[0].current["data-billid"]);
        let myradio = this.radios.find(q => q.current && q.current.getAttribute("billid") == billid && q.current.checked);
        //let myradio = this.radios.find(q => q.current && q.current.billid == billid && q.current.checked);
        let myvote = (myradio && myradio.current? myradio.current.value : null);
        //alert("bill: " + billid + ", vote: " + myvote);
        if (billid && myvote) {
            axios({
                method: 'get',
                withCredentials: true,
                url: window.appconfig.apiurl + "set_vote", 
                timeout: 400000,    // 4 seconds timeout
                params: {
                    billID: billid,
                    say: myvote
                } 
            })
            .then (res => {
                window.redux.dispatch({
                    type: "PROFILE",
                    payload: {
                        profile: res.data,
                        gbTimestamp: new Date()
                    }
                });
              this.load();
            });
        } else {
            alert("select the vote for bill " + billid);
        }
    }
    
    clickReloadBills() {
        this.reloadBills("Y");
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
        let saySpan = "";
        if (!bill.profileSay) {
            let sayAye = React.createRef();
            let sayNay = React.createRef();
            let sayPass = React.createRef();
            //bill.sayAye = sayAye;
            this.radios.push(sayAye);
            this.radios.push(sayNay);
            this.radios.push(sayPass);
            saySpan = <>
            <label><input name={bill.ID + "_say"} data-billid={bill.ID} billid={bill.ID} type="radio" ref={sayAye} defaultValue="AYE"/>AYE</label>
            <label><input name={bill.ID + "_say"} billid={bill.ID} type="radio" ref={sayNay} defaultValue="NAY"/>NAY</label>
            <label><input name={bill.ID + "_say"} billid={bill.ID} type="radio" ref={sayPass} defaultValue="PASS"/>PASS</label>
            <button billid={bill.ID} onClick={this.clickVote}>Vote</button>
            </>;
        } else {
            saySpan= <span>{bill.profileSay} on {bill.profileSayDate}</span>;
        }
        return (<>
        <tr key={bill.ID}>
            <td>{bill.ID}</td>
            <td>{bill.title}</td>
            <td>{bill.publishedDate}</td>
            {cells}
        </tr>
        <tr key={bill.iD + "_descr"}>
            <td></td>
            <td colSpan={gbState.groups.length + 3}>{bill.description}</td>
        </tr>
        <tr key={bill.ID + "_vore"}>
            <td></td>
            <td colSpan={gbState.groups.length + 3}>{saySpan}</td>
        </tr>
        </>);        
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

    testProfile() {
        /*
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_profile", 
            timeout: 400000,    // 4 seconds timeout
            params: {} 
        })
        .then (res => {
            //this.setState({bills: res.data});
            //this.load();
            alert(JSON.stringify(Utils.squash(res.headers)));
        });
        */
       /*
       fetch(window.appconfig.apiurl + "get_profile", {
           withCredentials: true,
           method: 'get',
           credentials: 'include'
       })
       .then(response => {
         //console.log(response.status);
         alert(JSON.stringify(Utils.squash(response)));
         return response.json();
       })
       .then(data => {
        //alert(JSON.stringify(Utils.squash(data)));
        })
       .catch(error => console.error(error))
       ; 
       */      
    }


    voteClick() {

    }

    reloadBills(forceArg) {
    
        axios({
            method: 'get',
            withCredentials: true,
            url: window.appconfig.apiurl + "get_bills", 
            timeout: 400000,    // 4 seconds timeout
            params: {force: forceArg} 
        })
        .then (res => {
            this.setState({bills: res.data});
        });
    }

    load() {
        this.reloadBills(null);
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
                    <button onClick={this.testBillClick}>Add Test Bill</button>
                    {/*<br/>
                    <button onClick={this.testProfile}>Test PRofile</button>
                    */}
                    </div>
                </div>
            );
        }
    }
}