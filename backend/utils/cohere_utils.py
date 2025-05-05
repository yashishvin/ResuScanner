import re
import cohere
import os
import json

co = cohere.Client(os.getenv("COHERE_API_KEY"))

def generate_questions(resume_analysis, jd_analysis):
    resume_json = json.dumps(resume_analysis, indent=2)
    jd_json = json.dumps(jd_analysis, indent=2)

    prompt = f"""
You are an AI assistant that helps candidates prepare for job interviews.

Here is the structured Resume Analysis:
{resume_json}

Here is the structured Job Description Analysis:
{jd_json}

Using the above information, generate **highly personalized technical and behavioral interview questions** tailored to:
1. The role and company described in the job description.
2. The candidate's actual experience and skill gaps.

Make sure to cover:
- Missing skills
- Relevant projects
- Technical and system design questions
- Company-specific cultural fit or behavioral questions
- Any important AWS/Kubernetes/DevOps details if relevant

Return 8–10 interview questions.
"""

    response = co.chat(
        model='command-r-plus',
        message=prompt,
        # max_tokens=500,
        temperature=0.7
    )

    return safe_extract_json(response.text)


def calculate_match_score(resume_analysis, jd_analysis):
    resume_json = json.dumps(resume_analysis, indent=2)
    jd_json = json.dumps(jd_analysis, indent=2)

    prompt = f"""
You are an expert hiring assistant. Analyze how well the candidate's resume aligns with the job description.

Resume Analysis:
{resume_json}

Job Description Analysis:
{jd_json}

Give a final **resume-to-job match score percentage** (0–100%) based on:
- Skill match
- Tools/technologies overlap
- Past experience relevance
- Culture fit
- Gaps or missing elements

Output format:
Match Score: XX%
"""

    response = co.chat(
        model='command-r-plus',
        message=prompt,
        # max_tokens=100,
        temperature=0.3
    )
    return safe_extract_json(response.text)

def safe_extract_json(text):
    match = re.search(r"```json\s*(\{.*\})\s*```", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(1))
        except json.JSONDecodeError:
            return {"error": "Invalid JSON in response", "raw": match.group(1)}
    return {"error": "No JSON block found", "raw": text}