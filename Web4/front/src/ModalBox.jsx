import React from 'react';

export class ModalBox extends React.Component {

    constructor(props) {
        super(props);        
        this.state = {
            show: false,
            closeOnVeil: props.closeOnVeil,
            onClose: props.onClose,
            onConfirm: props.onConfirm,
            content: props.content
        };
        this.doClose = this.doClose.bind(this);
        this.doOpen = this.doOpen.bind(this);
        this.doConfirm = this.doConfirm.bind(this);
    }

    doClose() {
        if (!this.state.show) {
            return;
        }
        if (typeof (this.state.onClose) == "function") {
            this.state.onClose();
        }
        this.setState({show: false});
    }

    doConfirm() {
        if (!this.state.show) {
            return;
        }
        if (typeof (this.props.onConfirm) == "function") {
            this.props.onConfirm();
        }
        this.setState({show: false});
    }

    doOpen() {
        if (this.state.show) {
            return;
        }
        this.setState({show: true});
    }
    render() {
        return (
        <div className="modal_veil" style={{display: this.state.show? "block" : "none"}} onClick={() => {if (this.state.closeOnVeil) this.doClose(); } }> 
            <div className="modal_box" onClick={(evt) => {evt.stopPropagation();}}>
            <div style={{height: '30px', borderBottom: '1px solid black'}}>
                <button style={{float: 'right', border: '1px solid black'}} onClick={this.doClose}>Close</button>
            </div>
            <div style={{width: '100%'}}>{this.props.children}</div>
            {typeof(this.props.onConfirm == "function")? (
                <div style={{textAlign: "center"}}>
                <button style={{}} onClick={this.doConfirm}>Confirm</button>
                <button style={{}} onClick={this.doClose}>Cancel</button>
                </div>) : ("")}
            </div>
        </div>
        );
    }
}