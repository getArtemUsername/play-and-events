import React from 'react';
import ReactDOM from 'react-dom';
import axios from 'axios';
import TagManager from './views/tag-manager.jsx';
import 'event-source-polyfill'
import {createStore} from "redux";
import {Provider} from "react-redux";
import NotificationService from './util/notification-service.js';
import {Router, Route, IndexRoute, IndexRedirect, browserHistory} from 'react-router';
import HomeComposite from './views/home-composite.jsx';
import AskQuestionView from './views/ask-question-view.jsx';
import QuestionComposite from './views/question-composite.jsx';
import QuestionListView from './views/question-list.jsx';
import QuestionDetailsView from './views/question-details-view.jsx'

class AppComponent {
    init = () => {
        const reactDiv = document.getElementById('reactDiv');
        if (!!reactDiv) {
            this.initLoginRedirecting();
            this.initAppState();
            //this.connectToSSEEndpoint();
            this.connectToWSEndpoint();
            this.renderComponent();
        }
    };

    initAppState = () => {
        console.info("initAppState");
        const initialState = {
            tags: [],
            questions: [],
            questionThread: {}
        };
        const reducer = (state = initialState, action) => {
            const updatedState = {...state};
            const actionType = action.type;

            if (actionType === 'tags_updated') {
                updatedState['tags'] = action.data;
            } else if (actionType === 'questions_updated') {
                updatedState['questions'] = action.data;
            } else if (actionType === 'question_thread_loaded') {
                updatedState['questionThread'] = action.data;
            } else if (actionType === 'question_thread_updated') {
                if (state['questionThread']['id'] === action.data['id']) {
                    updatedState['questionThread'] = action.data;
                }
            } else if (actionType === 'state_rebuild') {
                updatedState['refreshNeeded'] = true;
            }
            
            this.updateQuestionThreadId(action);
            return updatedState;
        };
        this.store = createStore(reducer);
    };

    updateQuestionThreadId = (action) => {
        this.useWS(() => {
            if (action.type == 'question_thread_updated' ||
                action.type == 'question_thread_loaded') {
            }
            const questionThreadId = action.data.question.id;
            this.streamWS.send(`{"questionThreadId": "${questionThreadId}"}`);
        });
    };

    useWS = (callback) => {
        if (this.streamWS.readyState === 1) {
            callback();
        } else {
            setTimeout(() => {
                this.useWS(callback);
            }, 1000);
        }
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

    connectToWSEndpoint = () => {
        this.streamWS = new WebSocket("ws://localhost:9000/api/wsStream");
        this.streamWS.onmessage = this.onServerSideEvent;
    };

    connectToSSEEndpoint = () => {
        this.es = new EventSource("/api/sse");
        console.info(this.es);
        this.es.addEventListener('message', this.onServerSideEvent);
    };

    onServerSideEvent = (event) => {
        console.info(event);
        if (event.type === 'message') {
            this.updateReceived(JSON.parse(event.data));
        }
    };

    updateReceived = (data) => {
        if (data['updateType'] === 'tags') {
            this.store.dispatch({
                type: 'tags_updated',
                data: data['updateData']
            });
        } else if (data['updateType'] === 'questions') {
            this.store.dispatch({
                type: 'questions_updated',
                data: data['updateData']
            });
        } else if (data['error'] != null) {
            NotificationService.showMessage({
                messageType: 'error',
                messageText: data['error']
            });
        } else if (data['updateType'] === 'questionThread') {
            this.store.dispatch({
                type: 'question_thread_updated',
                data: data['updateData']
            });
        } else if (data['updateType'] == 'stateRebuild') {
            this.store.dispatch({
                type: 'state_rebuild',
                data: true
            });
        }
    };


    renderComponent = () => {
        const reactDiv = document.getElementById('reactDiv');
        if (!!reactDiv) {
            ReactDOM.render(<Provider store={this.store}>
                <Router history={browserHistory}>
                    <Route path="/" component={HomeComposite}>
                        <IndexRedirect to="/ask"/>
                        <Route path="/tags" component={TagManager}/>
                        <Route path="/ask" component={AskQuestionView}/>
                        <Route path="/questions" component={QuestionComposite}>
                            <IndexRoute component={QuestionListView}/>
                            <Route path="/questions/:questionId"
                                   component={QuestionDetailsView}/>
                        </Route>
                    </Route>
                </Router>
            </Provider>, reactDiv);
        }
    }
}

export default AppComponent;
