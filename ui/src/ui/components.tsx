import * as React from "react";

export const UrlBox: React.SFC<{
    url: string,
    onChange: (url: string) => void,
}> = (props) => {
    return (
        <div className="form-group">
            <label htmlFor="urlField">Data Set URL</label>
            <input
                className="form-control"
                id="urlField" type="text"
                value={props.url}
                onChange={(e) => props.onChange(e.target.value)} />
        </div>
    );
};

export const ErrorBox: React.SFC<{ error: string }> = (props) => {
    return (
        <div className="alert alert-danger" role="alert">
            Conversion failed: {props.error}
        </div>
    );
};

export const ResultBox: React.SFC<{ file: string }> = (props) => {
    return (
        <div className="alert alert-success" role="alert">
            The data set was conmverted successfully.{" "}
            <a href={`/api/result/${props.file}`}
                title={props.file}
                className="alert-link">Click here to download it.</a>
        </div>
    );
};

export const ProgressBox: React.SFC<{}> = () => {
    return (
        <div className="alert alert-light" role="alert">
            Converting ...
            <div>
                <img src="progress.gif" />
            </div>
        </div>
    );
};