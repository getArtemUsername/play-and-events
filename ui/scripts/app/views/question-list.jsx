import * as React from "react";
import {connect} from 'react-redux';
import axios from 'axios';
import {Link} from 'react-router';
import ConfirmationService from '../util/confirmation-service.js';

class QuestionList extends React.Component {
    handlerResponse = (response) => {
        if (response.status === 200) {
            this.props.dispatch({
                type: 'questions_updated',
                data: response.data
            })
        }
    };

    componentDidMount = () => {
        axios.get('/api/questions').then(this.handlerResponse);
    };

    deleteQuestion = (questionId) => {
        return () => {
            ConfirmationService.showConfirmationDialog({
                title: 'Delete question',
                body: 'Are you sure you want to detele the question?'
            }, 'warning', () => {
                axios.post('/api/deleteQuestion', {'id': questionId})
            })
        };
    };

    render = () => {
        return <div className="question-list-container">
            <div className="question-list">
                {this.props.questions.map((question) => {
                    return <div key={question.id} className="question-list__one-question">
                        <div className="question-list__one-question__tags">
                            {question.tags.map((tag) => {
                                return <span className="label label-default"
                                             key={tag.id}>{tag.text}</span>
                            })}
                        </div>
                        <div className="question-list__one-question__title">
                            <Link to={"/questions/" + question.id}>{question.title}</Link>
                            <a href="#" onClick={this.deleteQuestion(question.id)}>
                                <span className="glyphicon glyphicon-remove"/>
                            </a>
                        </div>
                        <div className="question-list__one-question__author">
                            {"by " + question.authorFullName}
                        </div>
                    </div>
                })}
            </div>
        </div>
    }

}

const mapStateToProps = (state) => {
    return {
        questions: state.questions
    }
};

export default connect(mapStateToProps)(QuestionList)
