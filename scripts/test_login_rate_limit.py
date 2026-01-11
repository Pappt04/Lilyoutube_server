import requests
import time
import sys

# Configuration
BASE_URL = "http://localhost:8080/api/auth/login"
MAX_ATTEMPTS = 5
EMAIL = "test@example.com"
PASSWORD = "password123"

def run_test():
    print(f"Testing Login Rate Limit (strictly {MAX_ATTEMPTS} attempts per minute)")
    print("-" * 50)

    # 1. Send MAX_ATTEMPTS requests
    for i in range(1, MAX_ATTEMPTS + 1):
        try:
            response = requests.post(BASE_URL, json={"email": EMAIL, "password": PASSWORD})
            print(f"Attempt {i}: Status {response.status_code}")
            
            if response.status_code == 429:
                print(f"FAIL: Rate limited on attempt {i}, should allow {MAX_ATTEMPTS} attempts.")
                sys.exit(1)
        except requests.exceptions.ConnectionError:
            print("ERROR: Connection refused. Is the server running on http://localhost:8080?")
            sys.exit(1)

    # 2. Send the (MAX_ATTEMPTS + 1)-th request - should be rate limited
    print(f"Attempt {MAX_ATTEMPTS + 1}: ", end="", flush=True)
    response = requests.post(BASE_URL, json={"email": EMAIL, "password": PASSWORD})
    print(f"Status {response.status_code}")
    
    if response.status_code == 429:
        print("SUCCESS: 6th attempt correctly rate limited!")
    else:
        print(f"FAIL: 6th attempt should have been rate limited (Expected 429).")
        sys.exit(1)

    # 3. Wait for reset
    print("-" * 50)
    print("Waiting 61 seconds for the rate limit to reset...")
    time.sleep(61)

    # 4. Try again after reset
    print("Attempt after reset: ", end="", flush=True)
    response = requests.post(BASE_URL, json={"email": EMAIL, "password": PASSWORD})
    print(f"Status {response.status_code}")
    
    if response.status_code != 429:
        print("SUCCESS: Rate limit reset correctly!")
    else:
        print("FAIL: Still rate limited after waiting for 1 minute.")
        sys.exit(1)

    print("-" * 50)
    print("All tests passed!")

if __name__ == "__main__":
    run_test()
