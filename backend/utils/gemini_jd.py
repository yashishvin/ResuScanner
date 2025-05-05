import vertexai
from vertexai.preview.generative_models import GenerativeModel
import os
import json
import re

# Initialize Vertex AI
vertexai.init(project=os.getenv("GCP_PROJECT_ID"), location="us-central1")

# Load Gemini model
model = GenerativeModel(model_name="gemini-1.5-pro")

# Prompt Gemini to extract structured info
def analyze_job_description_gemini(jd_text: str) -> dict:
    prompt = f"""
    Extract structured job data from the following job description. Return ONLY a JSON object with:
    - company
    - role_title
    - responsibilities
    - required_skills
    - preferred_skills
    - location
    - compensation

    Job Description:
    {jd_text}
    """
    response = model.generate_content(prompt)
    text_output = response.text.strip()

    # 🔧 Remove code block markers if present
    match = re.search(r"```json\s*(\{.*\})\s*```", text_output, re.DOTALL)
    if match:
        text_output = match.group(1)

    try:
        return json.loads(text_output)
    except json.JSONDecodeError:
        return {"error": "Gemini response was not valid JSON", "raw": text_output}
