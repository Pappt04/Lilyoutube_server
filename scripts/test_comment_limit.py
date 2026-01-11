import requests
import json

def test_comment_limit():
    # Hardcoded configuration based on provided request
    BASE_URL = "http://localhost:8080"
    POST_ID = 2
    USER_ID = 2
    TOKEN = "64fe6cd2-ba79-403c-973b-4989885ed0a9"
    TEXT_PREFIX = "positive comment"
    
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
