import React from 'react';
import { TopMenu } from './TopMenu';

export function About() {
    return (
        <div>
            <h2>About the Project</h2>
            <div style={{textAlign: 'left'}}>
This is a test development project, used to:<p/>
<ul>
    <li>Maintain coding time with Java (currently 1.8) and JSP</li>
    <li>Practice with React and other fun things</li>
    <li>Implement a game-like dynamic system, interesting to me personally</li>
    </ul>                
    <p/>
        Gaming value of this project is currently nil, and the website state is very much under construction. Both will hopefully change for the better, soon.
        <p/>
        Cheers.
            </div>
        </div>
    )
}