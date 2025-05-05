from flask import Flask
from flask_cors import CORS
from resume import resume_api
from jd import jd_api
from matchscore import match_score_api
from questions import questions_api


def create_app():
    app = Flask(__name__)
    CORS(app)  # Allow Android to access
    app.register_blueprint(resume_api)
    app.register_blueprint(jd_api)
    app.register_blueprint(match_score_api)
    app.register_blueprint(questions_api)
    return app

if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=5001)
