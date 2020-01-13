import React from 'react';
import { BrowserRouter,  Redirect, Link, Route, Switch } from "react-router-dom";

import logo from './logo.svg';
import './App.css';
import {Groups} from './Groups'
import { TopMenu } from './TopMenu';
import {Front} from './Front';
import {About} from './About';

function App() {
  return (
    <div>
    <TopMenu/>
    <BrowserRouter basename={window.appconfig.public_url}>
    {/*
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <h2>My title: {window.appconfig.apptitle}</h2>
        <Groups/>
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
        */}
        <Route exact path="/" component={Front}/>
        <Route path="/groups" component={Groups}/>
        <Route path="/about" component={About}/>
        </BrowserRouter>
    </div>
  );
}

export default App;
