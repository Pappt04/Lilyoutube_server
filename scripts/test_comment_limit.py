import requests
import json
import sys

def authenticate():
    """Login with credentials and get token."""
    login_payload = {
        "email": "papi@gmail.com",
        "password": "papipapi"
    }

    try:
        resp = requests.post("http://localhost:8080/api/auth/login", json=login_payload)
        if resp.status_code == 200:
            token = resp.json().get("token")
            print(f"Login successful! Token: {token}")
            return token
        else:
            print(f"Login failed ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"Login error: {e}")
        return None

def test_comment_limit():
    # Hardcoded configuration based on provided request
    BASE_URL = "http://localhost:8080"
    POST_ID = 2
    USER_ID = 2
    TEXT_PREFIX = "positive comment"

    # Authenticate first
    TOKEN = authenticate()
    if not TOKEN:
        print("ERROR: Failed to authenticate. Exiting.")
        sys.exit(1)

    headers = {
        "Authorization": f"Bearer {{\"token\":\"{TOKEN}\"}}",
        "Content-Type": "application/json"
    }
    
    comment_url = f"{BASE_URL}/api/comments"
    print(f"Testing comment rate limit at {comment_url}")
    print(f"Using Bearer token: {TOKEN}")
    print(f"Target Post ID: {POST_ID}, User ID: {USER_ID}")
    
    # Try to post more than 60 comments (up to 65)
    for i in range(1, 66):
        payload = {
            "user_id": USER_ID,
            "post_id": POST_ID,
            "text": f"{TEXT_PREFIX} {i}"
        }
        
        try:
            resp = requests.post(comment_url, headers=headers, json=payload)
            
            if resp.status_code == 200:
                print(f"[{i}] Comment posted successfully.")
            elif resp.status_code == 429:
                print(f"[{i}] Hit rate limit as expected: 429 Too Many Requests")
                print(f"Response: {resp.text}")
                break
            else:
                print(f"[{i}] Unexpected status code: {resp.status_code}")
                print(f"Response: {resp.text}")
                break
        except Exception as e:
            print(f"[{i}] Request failed: {e}")
            break

if __name__ == "__main__":
    test_comment_limit()
