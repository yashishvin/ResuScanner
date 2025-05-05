from flask import Blueprint, request, jsonify
from utils.db import get_collection
from utils.cohere_utils import calculate_match_score

match_score_api = Blueprint("match_score_api", __name__)
resume_outputs_col = get_collection("resume_outputs")
jd_outputs_col = get_collection("jd_outputs")

@match_score_api.route("/match-score", methods=["POST"])
def match_score():
    try:
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

        score = calculate_match_score(resume_analysis, jd_analysis)

        return jsonify({"session_id": session_id, "match_score": score})
    except Exception as e:
        print(f"🔥 Error in /match-score: {e}")
        return jsonify({"error": str(e)}), 500
