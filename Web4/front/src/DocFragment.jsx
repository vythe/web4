import React from 'react';
import axios from 'axios';
import {Utils} from './Utils';

export class DocFragment extends React.Component {

    /*
    Expected props:
    hashtag : string
    */
    constructor(props) {

        super(props);

        this.state = {
            hashtag: this.props.hashtag,
            content: "(Loading " + this.props.hashtag + "...)"
        }        
    }

    componentDidMount() {
        let url = Utils.localURL("/docs/"); //docs.html";
        if (this.state.hashtag) {
            //url += "#" + this.state.hashtag; // it should be a simple string, possibly with ":"
            url += this.state.hashtag.replace(":", "/") + ".html";
        } else {
            url += "root.html";
        }
        axios({
            method: 'get',
            withCredentials: false,
            url: url, 
            timeout: 400000    // 4 seconds timeout
            //, params: {force: forceArg} 
        })
        .then (res => {
            this.setState({content: res.data });
         });
    }

    render() {
        return (<div className="appdocs"  dangerouslySetInnerHTML={{__html: this.state.content}} >
        </div>);
    }
}