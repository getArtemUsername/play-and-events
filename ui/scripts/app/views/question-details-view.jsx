import React from 'react';
import axios from 'axios';
import {connect} from 'react-redux';
import Modal from 'react-modal';
import EditAnswerForm from './edit-answer-form.jsx';
import ConfirmationService from '../util/confirmation-service.js';
import moment from 'moment';

class QuestionDetailsView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            editAnswerIsOpen: false
        }
    }

    openEditAnswerForm = () => {
        this.setState({
            editAnswerIsOpen: true
        });
    };
    closeEditAnswerForm = () => {
        this.setState({
            editAnswerIsOpen: false
        });
    };

    deleteAnswer = (questionId, answerId) => {
        return () => {
            ConfirmationService.showConfirmationDialog({
                title: 'Delete answer',
                body: 'Are u sure u want to delete the answer?'
            }, 'warning', () => {
                axios.post("/api/deleteAnswer", {
                    "answerId": answerId,
                    "questionId": questionId
                })
            });
        };
    };

    upvoteAnswer = (questionId, answerId) => {
        return () => {
            axios.post("/api/upvoteAnswer", {
                "answerId": answerId,
                "questionId": questionId
            })
        }
    };

    downvoteAnswer = (questionId, answerId) => {
        return () => {
            axios.post("/api/downvoteAnswer", {
                "answerId": answerId,
                "questionId": questionId
            });

        }

    };
    componentDidMount = () => {
        const questionId = this.props.params['questionId'];
        axios.get(`/api/questionsThread/${questionId}`).then(this.handleResponse);
    };

    handleResponse = (response) => {
        if (response.status === 200) {
            this.props.dispatch({
                type: 'question_thread_loaded',
                data: response.data
            });
        }
    };

    render = () => {
        if (this.props.questionThread === null ||
            this.props.questionThread.question === null) {
            return <div className="question-thread-view-form">
                <div className="question-thread-view-form__body">
                    <div className="question-thread-view-form__loading">Loading...</div>
                </div>
            </div>
        }
        const question = this.props.questionThread.question;
        const userIdHolder = document.getElementsById('data-user-id-holder');
        const maybeUserId = (userIdHolder !== null) ?
            userIdHolder.getAttribute('data-user-id') : null;
        const userNotLoggedIn = maybeUserId === null;

        const answers = this.props.questionThread.answers;
        const answerInd = answers.findIndex((answer) => {
            return answer.authorId = maybeUserId;
        });
        const answerExists = answerInd !== -1;
        const maybeAnswer = answerExists ? answers[answerInd] : null;
        const editAnswerText = answerExists ? 'Edit answer' : 'Add answer';

        const answerEditStyle = {
            content: {
                maxWidth: "600px",
                margin: "0 auto",
                height: "400px",
                position: "relative"
            }
        };

        return <div className="question-thread-view-form">
            <div className="question-thread-view-form__body">
                <div className="question-thread-view-form__tags">
                    {question.tags.map((tag) => {
                        return <span className="label label-default" key={tag.id}>{tag.text}</span>
                    })}
                </div>
                <div className="question-thread-view-form__title">
                    {question.title}
                </div>
                <div className="question-thread-view-form__details">
                    {question.details}
                </div>
                <button className="btn btn-default save-button"
                        disabled={userNotLoggedIn}
                        onClick={this.openEditAnswerForm}
                        type="button">{editAnswerText}</button>
            </div>
            <Modal isOpen={this.state.editAnswerIsOpen} onRequestClose={this.closeEditAnswerForm}
                   style={answerEditStyle} contentLabel="Edit answer">
                <EditAnswerForm maybeAnswer={maybeAnswer} questionId={question.id}
                                onAnswerUpdated={this.closeEditAnswerForm}/>
            </Modal>
        </div>


    }
}

const mapStateToProps = (state) => {
    return {
        questionThread: state.questionThread
    }
};

export default connect(mapStateToProps)(QuestionDetailsView);
