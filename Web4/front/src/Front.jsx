import React from 'react';
import axios from 'axios';
import {Utils} from './Utils'
import {TopMenu} from './TopMenu';
import { AffinityEdit } from './AffinityEdit';
import {WebUser} from './WebUser';
import { UserProfile } from './UserProfile';
import {Bills} from './Bills';

export class Front extends React.Component {
    render() {
        return (
            <div>
            <div className="error">Under construction</div>
            <WebUser/>
            <UserProfile/>
            <Bills/>
            </div>
        );
    }
}