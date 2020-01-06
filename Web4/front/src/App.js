import React from 'react';
import logo from './logo.svg';
import './App.css';
import {Groups} from './Groups'

function App() {
  return (
    <div className="App">
      <header className="App-header">
      {/*
        <img src={logo} className="App-logo" alt="logo" />
        */}
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
  );
}

export default App;
