from flask import Blueprint, request, jsonify
from utils.db import get_collection
from utils.gemini_resume import analyze_resume
import fitz
import uuid

resume_api = Blueprint("resume_api", __name__)
resumes_col = get_collection("resumes")
resume_outputs_col = get_collection("resume_outputs")

@resume_api.route("/upload-resume", methods=["POST"])
def upload_resume():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file part in request"}), 400

        file = request.files['file']
        if file.filename == '':
            return jsonify({"error": "No selected file"}), 400

        if not file.filename.lower().endswith('.pdf'):
            return jsonify({"error": "Only PDF files are supported"}), 400

        resume_text = extract_text(file)
        # save_resume_to_mongodb(file.filename, resume_text)

        session_id = str(uuid.uuid4())
        resumes_col.insert_one({
            "session_id": session_id,
            "filename": file.filename,
            "resume_text": resume_text
        })

        return jsonify({"status": "saved", "filename": file.filename,"session_id": session_id})

    except Exception as e:
        print(f"🔥 Upload Resume Error: {e}")
        return jsonify({"error": str(e)}), 500

@resume_api.route("/analyze-resume", methods=["POST"])
def analyze_resume_api():
    try:
        data = request.get_json()
        # resume_text = data.get("resume_text")
        session_id = data.get("session_id")
        user_id = data.get("user_id", "spartan@sjsu.edu")

        resume_doc = resumes_col.find_one({"session_id": session_id})
        # if not resume_text or not session_id:
        #     return jsonify({"error": "Missing resume text or session ID"}), 400
        if not resume_doc:
            return jsonify({"error": "Resume not found"}), 404

        result = analyze_resume(resume_doc["resume_text"])
        resume_outputs_col.insert_one({
            "session_id": session_id,
            "user_id": user_id,
            "analysis": result
        })
        return jsonify({"status": "analyzed", "session_id": session_id, "analysis": result})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def extract_text(file):
    doc = fitz.open(stream=file.read(), filetype="pdf")
    return "".join([page.get_text() for page in doc])

def save_resume_to_mongodb(filename, resume_text):
    session_id = str(uuid.uuid4())
    record = {
        "session_id": session_id,
        "filename": filename,
        "resume_text": resume_text
    }
    resumes_col.insert_one(record)
