import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import TagManager from './views/tag-manager.jsx';
import 'event-source-polyfill'
import {createStore} from "redux";
import {Provider} from "react-redux";


class AppComponent {
    init = () => {
        this.initLoginRedirecting();
        this.initAppState();
        this.connectToSSEEndpoint();
        this.renderComponent();
    };

    initAppState = () => {
        console.info("initAppState");
        const initialState = {
            tags: []
        };
        const reducer = (state = initialState, action) => {
            const updatedState = {...state};
            const actionType = action.type;

            if (actionType === 'tags_updated') {
                updatedState['tags'] = action.data
            }
            return updatedState;
        };
        this.store = createStore(reducer);
    };

    initLoginRedirecting = () => {
        axios.interceptors.response.use((response) => {
            return response;
        }, (error) => {
            if (error.response.status === 401) {
                window.location = '/login';
            }
            return Promise.reject(error);
        });
    };

    connectToSSEEndpoint = () => {
        console.info("connectToSSEEndpoint");
        this.es = new EventSource("/api/sse");
        console.info(this.es);
        this.es.addEventListener('message', this.onServerSideEvent);
    };

    onServerSideEvent = (event) => {
        console.info("onServerSideEvent ");
        console.info(event);
        if (event.type === 'message') {
            this.updateReceived(JSON.parse(event.data));
        }
    };

    updateReceived = (data) => {
        console.info("Update received data ");
        console.info(data);
        if (data['updateType'] === 'tags') {
            this.store.dispatch({
                type: 'tags_updated',
                data: data['updateData']
            });
        }
    };


    renderComponent = () => {
        const reactDiv = document.getElementById('reactDiv');
        if (!!reactDiv) {
            ReactDOM.render(<Provider store={this.store}>
                <TagManager />
            </Provider>, reactDiv);
        }
    }
}

export default AppComponent;
