import React from 'react';

export class ModalBox extends React.Component {

    constructor(props) {
        super(props);        
        this.state = {
            show: false,
            closeOnVeil: props.closeOnVeil,
            onClose: props.onClose,
            content: props.content
        };
        this.doClose = this.doClose.bind(this);
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

    render() {
        return (
        <div className="modal_veil" style={{display: this.state.show? "block" : "none"}} onClick={() => {if (this.state.closeOnVeil) this.doClose(); } }> 
            <div className="modal_box" onClick={(evt) => {evt.stopPropagation();}}>
            <div style={{height: '30px', borderBottom: '1px solid black'}}>
                <button style={{float: 'right', border: '1px solid black'}} onClick={this.doClose}>Close</button>
            </div>
            <div style={{width: '100%'}}>{this.props.children}</div>
            </div>
        </div>
        );
    }
}