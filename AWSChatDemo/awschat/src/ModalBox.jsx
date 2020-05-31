import React from 'react';

export class ModalBox extends React.Component {

    /*
    Expectd props:  
    closeOnVeil
    onClose: function
    onConfirm: function
    title: string
    labelClose: string
    labelConfirm: string
    */
    constructor(props) {
        super(props);        
        this.state = {
            show: false,
            closeOnVeil: props.closeOnVeil,
            onClose: props.onClose,
            onConfirm: props.onConfirm
            //, content: props.content
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
        let needBottomDiv = (typeof(this.props.onConfirm) == "function" || this.props.labelClose || this.props.labelConfirm);

        return (
        <div className="modal_veil" 
            style={{display: this.state.show? "block" : "none"}}  
            tabIndex={-1}
            ref={(divRef) => { divRef && divRef.focus()}}       
            onClick={() => {if (this.state.closeOnVeil) this.doClose(); } }
            onKeyUp={(evt) => { if (this.state.closeOnVeil && evt.key == "Escape") this.doClose();}}
        > 
            <div className="modal_box" 
                onClick={(evt) => {evt.stopPropagation();}}
            >
            <div style={{height: '30px', borderBottom: '1px solid black', textAlign: "center"}}>
                <span>{this.props.title}</span>
                <button style={{float: 'right', border: '1px solid black'}} onClick={this.doClose}>Close</button>
            </div>
            <div style={{width: '100%'}}>{this.props.children}</div>
            {needBottomDiv? (
                <div style={{textAlign: "center"}}>
                    {typeof(this.props.onConfirm) == "function"? (
                        <button style={{}} onClick={this.doConfirm}>{this.props.labelConfirm || "Confirm"}</button>
                    ) : ("")}
                    {this.props.labelClose? (
                <button style={{}} onClick={this.doClose}>{this.props.labelClose || "Cancel"}</button>
                    ) : ("")}
                <br/>
                </div>
                ) : ("")
            }
            </div>
        </div>
        );
    }
}