# backend/jd.py

import uuid
from flask import Blueprint, request, jsonify
from utils.db import get_collection
from utils.gemini_jd import analyze_job_description_gemini

jd_api = Blueprint("jd_api", __name__)
jd_col = get_collection("job_descriptions")
jd_outputs_col = get_collection("jd_outputs")

@jd_api.route("/analyze-jd", methods=["POST"])
def analyze_jd_api():
    try:
        data = request.get_json()
        session_id = data.get("session_id")
        jd_text    = data.get("jd_text")

        # 1️⃣ Validate inputs
        if not session_id:
            return jsonify({"error": "Missing session_id"}), 400
        if not jd_text:
            return jsonify({"error": "Missing jd_text"}), 400

        # 2️⃣ Store the JD under the existing session_id
        jd_col.insert_one({
            "session_id": session_id,
            "jd_text": jd_text
        })

        # 3️⃣ Run Gemini analysis
        result = analyze_job_description_gemini(jd_text)

        # 4️⃣ Save the analysis output under the same session_id
        jd_outputs_col.insert_one({
            "session_id": session_id,
            "analysis": result
        })

        # 5️⃣ Return the session_id and the analysis to the client
        return jsonify({
            "status": "analyzed",
            "session_id": session_id,
            "analysis": result
        })

    except Exception as e:
        print(f"🔥 Error in analyze_jd_api: {e}")
        return jsonify({"error": str(e)}), 500
