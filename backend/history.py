# backend/history.py

from flask import Blueprint, request, jsonify
from utils.db import get_collection

history_api = Blueprint("history_api", __name__)
match_scores_col = get_collection("match_scores")

@history_api.route("/history", methods=["POST"])
def get_history():
    data = request.get_json()
    user_id = "spartan@sjsu.com"
    if not user_id:
        return jsonify({"error": "Missing user_id"}), 400

    # find all match‐score records for this user
    cursor = match_scores_col.find({"user_id": user_id})
    history = []
    for doc in cursor:
        history.append({
            "company":    doc.get("company"),
            "role_title": doc.get("role_title"),
            "match_score": doc.get("match_score")
        })

    return jsonify(history), 200
