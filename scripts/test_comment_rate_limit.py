import requests
import time
import sys

# Configuration
POST_ID = 1
USER_ID = 1
BASE_URL = "http://localhost:8080/api"

def authenticate():
    """Login with credentials and get token."""
    login_payload = {
        "email": "papi@gmail.com",
        "password": "papipapi"
    }

    try:
        resp = requests.post(f"http://localhost:8080/api/auth/login", json=login_payload)
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

def test_rate_limit():
    print("Testing comment rate limiting...")

    # Authenticate first
    TOKEN = authenticate()
    if not TOKEN:
        print("ERROR: Failed to authenticate. Exiting.")
        sys.exit(1)

    print(f"Post ID: {POST_ID}")
    print(f"User ID: {USER_ID}")
    print(f"Base URL: {BASE_URL}")
    print("")

    success_count = 0
    fail_count = 0
    rate_limit_count = 0

    headers = {
        "Content-Type": "application/json",
        "Authorization": f'Bearer {{"token":"{TOKEN}"}}'
    }

    # Try to send 70 comments (10 more than the limit)
    for i in range(1, 71):
        print(f"Sending comment {i}... ", end="", flush=True)
        
        payload = {
            "post_id": POST_ID,
            "user_id": USER_ID,
            "text": f"Test comment {i}"
        }
        
        try:
            response = requests.post(f"{BASE_URL}/comments", headers=headers, json=payload)
            status_code = response.status_code
            
            if status_code == 200:
                print("SUCCESS")
                success_count += 1
            elif status_code == 429:
                print("RATE LIMIT (429)")
                rate_limit_count += 1
                fail_count += 1
            elif status_code == 401:
                print("UNAUTHORIZED (401)")
                fail_count += 1
            else:
                print(f"FAILED (HTTP {status_code})")
                print(f"Response: {response.text}")
                fail_count += 1
        except Exception as e:
            print(f"ERROR: {e}")
            fail_count += 1
        
        # Small delay to avoid overwhelming the server
        time.sleep(0.1)

    print("")
    print("=========================================")
    print("Test Results:")
    print(f"Successful comments: {success_count}")
    print(f"Rate limited (429): {rate_limit_count}")
    print(f"Failed comments: {fail_count}")
    print("=========================================")
    print("")
    print("Expected: First 60 comments should succeed, remaining should be rate limited (429)")

if __name__ == "__main__":
    test_rate_limit()
