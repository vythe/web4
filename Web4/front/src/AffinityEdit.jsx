import React from 'react';

export class AffinityEdit extends React.Component
{

    constructor(props) {
        super(props);
        // we want these props: moniker, tomoniker, name, toname, value
        this.state = {
            moniker: props.moniker,
            toMoniker: props.tomoniker,
            name: props.name,
            toName: props.toname,
            value: +props.value
        };
        if (this.state.value < 0 || this.state_value > 1) {
            this.state.value = null;
        }
        this.oldValue = this.state.value;
        //this.radioVal = null;

        this.submitClick = this.submitClick.bind(this);
        this.radioChange = this.radioChange.bind(this);
        this.radioVal1 = React.createRef();
        this.radioVal2 = React.createRef();
        this.radioAssign = (this.state.value != null);
    }

    componentDidMount() {
        this.radioVal1.current.checked = (this.state.value != null);
        this.radioVal2.current.checked = (this.state.value == null);
    }

    submitClick() {
        alert("radioval1: " + this.radioVal1.current.value + ", checked1:" + this.radioVal1.current.checked
        //+ "\nradioval2: " + this.radioVal2.current.value + ", checked2:" + this.radioVal2.current.checked
        );
    }

    radioChange() {

    }
    render() {
        if (!this.state.moniker || !this.state.toMoniker) {
            return (
            <div className="error">Invalid affinity data</div>                
            );
        } else {
            let radioName = "radio-" + this.state.moniker + "-" + this.state.toMoniker;
            return (
            <div>
                <label><input ref={this.radioVal1} type="radio" name={radioName} value="S" />Assign</label><br/>
                <label><input ref={this.radioVal2} type="radio" name={radioName} value="N"/>Auto-Calculate</label><br/>
                <button onClick={this.submitClick}>Check radio</button>
            </div>
            );
        }
    }
}