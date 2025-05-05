import google.generativeai as genai
import os
from dotenv import load_dotenv
import json

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
model = genai.GenerativeModel(model_name="gemini-1.5-flash")


def analyze_resume(resume_text: str) -> dict:
    # model = genai.Model(model_name="gemini-1.5-pro")
    # response = model.generate_content(f"Extract structured skills, experience, education from this resume: {resume_text}")
    # return response.text
    prompt = f"""
    You are a resume analysis assistant. Analyze the following resume content and return a JSON object with these fields:

    Given this resume text, extract:
    - summary
    - technical_skills (as a list)
    - soft_skills (as a list)
    - tools (as a list)
    - work_experience (as a list of dicts with company, role, duration)
    - gaps (as a list of improvement suggestions)

    Return only valid JSON.

    Resume Text:
    {resume_text}
    """
    try:
        response = model.generate_content(prompt)
        cleaned = response.text.strip("```json\n").strip("```")
        return json.loads(cleaned)

    except Exception as err:
        print("🔥 Gemini JSON parsing error:", err)

        fallback_text = response.text if 'response' in locals() else "No Gemini response."
        return {
            "error": "Gemini response was not valid JSON",
            "raw": fallback_text
        }
