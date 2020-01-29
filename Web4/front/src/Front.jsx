import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'
import {TopMenu} from './TopMenu';
import { AffinityEdit } from './AffinityEdit';
import {WebUser} from './WebUser';
import { UserProfile } from './UserProfile';
import {Bills} from './Bills';
import {ModalBox} from './ModalBox';

export class Front extends React.Component {

    constructor(props) {
        super(props);
        this.myModal1 = React.createRef();
        this.myModal2 = React.createRef();
        this.myB = React.createRef();
        this.state = {};

        this.doShowModal = this.doShowModal.bind(this);
    }

    doShowModal(option) {
        //this.setState({message: <b>from front</b>});
        //this.myB.appendChild(<span>my <i>other</i> front</span>);
        this.myB.current.innerHTML = "<span>my <i>other</i> front</span>";
        if (option == "1") {
            this.myModal1.current.setState({show: true});
        } else {
            this.myModal2.current.setState({show: true});
        }

    }
    render() {
        return (
            <div>
            <div className="error">Under construction</div>
            <WebUser/>
            <UserProfile/>
            <Bills/>
            <hr/>
            <button onClick={() => {this.doShowModal("1"); }}>Show modal1</button>
            <button onClick={() => this.doShowModal("2")}>Show modal2</button>
            <ModalBox ref={this.myModal1}>Hello, Modal1 <b ref={this.myB}></b> </ModalBox>
            <ModalBox ref={this.myModal2}>Hello, Modal2 <b ref={this.myB}></b><br/>
            <button onClick={() => this.myModal2.current.setState({show: false})}>Close Modal2</button>
             </ModalBox>
            </div>
        );
    }
}