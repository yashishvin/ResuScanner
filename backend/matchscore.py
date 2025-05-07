from flask import Blueprint, request, jsonify
from utils.db import get_collection
from utils.cohere_utils import calculate_match_score
import re

match_score_api = Blueprint("match_score_api", __name__)
resume_outputs_col = get_collection("resume_outputs")
jd_outputs_col = get_collection("jd_outputs")
match_scores_col = get_collection("match_scores")

@match_score_api.route("/match-score", methods=["POST"])
def match_score():
    try:
        user_id = "spartan@sjsu.com"
        data = request.get_json()
        session_id = data.get("session_id")

        if not session_id:
            return jsonify({"error": "Missing session ID"}), 400
        
        resume_doc = resume_outputs_col.find_one({"session_id": session_id})
        jd_doc = jd_outputs_col.find_one({"session_id": session_id})

        if not resume_doc or not jd_doc:
            return jsonify({"error": "Missing resume or JD analysis for this session ID"}), 404

        resume_analysis = resume_doc["analysis"]
        jd_analysis = jd_doc["analysis"]

        # this returns a dict like {"error":..., "raw":"Match Score: 75% …"}
        raw_score = calculate_match_score(resume_analysis, jd_analysis)

        # extract integer percentage
        # get the text to scan
        text = ""
        if isinstance(raw_score, dict):
            text = raw_score.get("raw", "")
        else:
            text = str(raw_score)

        # extract the first integer percentage
        m = re.search(r"(\d+)%", text)
        percent = int(m.group(1)) if m else 0

        # insert the integer percentage
        match_scores_col.insert_one({
            "user_id":     user_id,
            "session_id":  session_id,
            "company":     jd_doc["analysis"].get("company"),
            "role_title":  jd_doc["analysis"].get("role_title"),
            "match_score": percent
        })

        return jsonify({"session_id": session_id, "match_score": raw_score})
    
    except Exception as e:
        print(f"🔥 Error in /match-score: {e}")
        return jsonify({"error": str(e)}), 500
