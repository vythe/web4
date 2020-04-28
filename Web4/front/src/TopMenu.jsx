import React from 'react';
import { Utils } from './Utils';
import { BrowserRouter, Redirect, Link, Route, Switch } from "react-router-dom";

export class TopMenu extends React.Component
{

    render() {
        // we could use this Link form: <Link to="/" className="menu_link">Front1</Link>
        return (
            <div>
                        <h1>Speaking Up Playground</h1>
            <div className="curved container" style={{backgroundImage: Utils.cssURL("/img/grad_grey.png"), backgroundSize : '100% 100%' }}>
                <span className="menu_item_left">                    
                    <a className="menu_link" href={Utils.localURL("/")}>Front</a>
                </span>
                <span className="menu_item_left">
                    <a className="menu_link" href={Utils.localURL("/groups")}>Groups</a>
                </span>
                <span className="menu_item_left">
                    <a className="menu_link" href={Utils.localURL("/docs/docs.html")}>Docs</a>
                </span>

                <span className="menu_item_right">
                    <a className="menu_link" href={Utils.localURL("/about")}>About</a>
                </span>
                <br className="separator"/>
            </div>
            </div>
        );
    }
}