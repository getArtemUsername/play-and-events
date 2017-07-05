import React from 'react';
import axios from 'axios';
import {connect} from "react-redux";

class TagManager extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            text: ''
        };
    };

    componentDidMount = () => {
        console.info("tag manager mount");
        axios.get('/api/tags').then(this.handleResponse);
    };

    handleResponse = (response) => {
        console.info("handleResponse");
        console.info(response);
        if (response.status === 200) {
            this.props.dispatch({
                type: 'tags_updated',
                data: response.data
            });
        } else {
            console.error(response.statusText);
        }
    };

    addTag = () => {
        console.info("addTag");
        const text = this.state.text;
        const isValid = this.props.tags.findIndex((el) => {
                return el.text === text;
            }) === -1;
        if (isValid) {
            axios.post('/api/createTag', {'text': text}).then(() => {
                this.setState({
                   text: '' 
                });
            });
        }
    };

    deleteTag = (id) => {
        console.info("deleteTag" + id);
        return () => {
            axios.post('/api/deleteTag', {'id': id});
        };
    };

    handleInput = (event) => {
        console.info("handleInput " + event.toString());
        this.setState({
            text: event.target.value
        });
    };

    handleKeyPress = (event) => {
        if (event.key === 'Enter') {
            console.info("Press Enter");
            this.addTag();
        }
    };

    render = () => {
        const tags = this.props.tags;
        console.info(tags);
        console.info(this.props.tags);
        return <div className="tag-manager">
            <div className="tag-manager__input-panel">
                <div className="tag-manager__input-panel_input">
                    <input type="text" className="form-control"
                           onKeyPress={this.handleKeyPress}
                           placeholder="Enter a new tag and press enter"
                           value={this.state.text}
                           onChange={this.handleInput}/>
                </div>
            </div>
            <div className="tag-manager__cloud-panel">
                <div className="tag-manager__cloud-panel__available-tags">
                    {tags.map((tag) => {
                        return <span className="label label-primary"
                                     key={tag.id}>{tag.text}
                            <a className="remove-tag-link"
                               onClick={this.deleteTag(tag.id)}>x</a></span>
                    })}
                </div>

            </div>
        </div>
    };


}

const mapStateToProps = (state) => {
    console.info("mapStateToProps ");
    console.info(state);
    return {tags: state.tags}
};

export default connect(mapStateToProps)(TagManager);
