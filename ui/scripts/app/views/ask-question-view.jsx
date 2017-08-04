import React from 'react';
import {connect} from 'react-redux';
import axios from 'axios';
import Select from "react-select";

class AskQuestionView extends React.Component {

    constructor(props) {
        super(props);
        this.state = this.getInitState();
    };

    getInitState = () => {
        return {
            tags: [],
            title: '',
            details: ''
        }
    };

    handleResponse = (response) => {
        if (response.status === 200) {
            this.props.dispatch({
                type: 'tags_updated',
                data: response.data
            })
        }
    };

    handleCombo = (value) => {
        this.setState({
            tags: value
        });
    };

    handleChange = (key) => {
        return event => {
            const newState = {};
            newState[key] = event.target.value;
            this.setState(newState);
        }
    };

    componentDidMount = () => {
        axios.get('/api/tags').then(this.handleResponse);
    };
    
    createQuestion = () => {
        const tagCodes = this.state.tags.map((opt) => {
            return opt.value;
        });
        const newQuestion = {
            title: this.state.title,
            tags: tagCodes,
            details: this.state.details
        };
        axios.post("/api/createQuestion", newQuestion).then((res) => {
            if (res.status = 200) {
                this.setState(this.getInitState());
                this.props.history.push('/question');
            }
        });
    };

    render = () => {
        const tagOptions = this.props.tags.map((tag) => {
            return {
                value: tag.id,
                label: tag.text
            }
        });
        const buttonDisabled = this.state.tags.length === 0 ||
            this.state.title.length === 0;
        return <div className="question-view-form">
            <div className="question-view-form__body-panel__title-panel__input">
                <input type="text" value={this.state.title} onChange={this.handleChange('title')}/>
                <Select onChange={this.handleCombo} value={this.state.tags}
                        multi={true} clearable={true} options={tagOptions}
                        placeholder="Please select tags for the question"/>
                <textarea className="form-control" rows="3" value={this.state.details}/>
            </div>
            <div className="question-view-form__body-panel__title-panel__button">
                <button className="btn btn-primary" disabled={buttonDisabled}
                        onClick={this.createQuestion}>Submit</button>

            </div>
        </div>
    };
}

const mapStateToProps = (state) => {
    return {
        tags: state.tags
    };
};

export default connect(mapStateToProps)(AskQuestionView)
