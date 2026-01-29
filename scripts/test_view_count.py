import requests
import time
import os
import re
import sys

# Colors for output
GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m'

def get_token_from_scripts():
    """
    Attempts to find an auth token in other python scripts in the same directory.
    """
    scripts_dir = os.path.dirname(os.path.abspath(__file__))
    # List all python files in the scripts directory
    for filename in os.listdir(scripts_dir):
        if filename.endswith(".py") and filename != os.path.basename(__file__):
            try:
                with open(os.path.join(scripts_dir, filename), "r") as f:
                    content = f.read()
                    # Look for TOKEN = "..." or BEARER_TOKEN = "..."
                    match = re.search(r'(?:TOKEN|BEARER_TOKEN)\s*=\s*["\']([^"\']+)["\']', content)
                    if match:
                        print(f"{GREEN}✓ Found token in {filename}{NC}")
                        return match.group(1)
            except Exception as e:
                continue
    return None

def main():
    print(f"{BLUE}=========================================================={NC}")
    print(f"{BLUE}          View Counting Synchronization Test              {NC}")
    print(f"{BLUE}=========================================================={NC}")
    
    # Configuration
    APP1_URL = "http://localhost:8081"
    APP2_URL = "http://localhost:8082"
    
    # Step 0: Get the token
    token = get_token_from_scripts()
    if not token:
        print(f"{RED}Error: Could not find an authentication token in existing scripts.{NC}")
        print(f"{YELLOW}Please ensure at least one script in this folder has a TOKEN variable.{NC}")
        sys.exit(1)
    
    headers = {
        "Authorization": f'Bearer {{"token":"{token}"}}',
        "Content-Type": "application/json"
    }

    # Step 1: Find a video to test with
    print(f"\n{BLUE}[Step 1] Fetching videos to select target...{NC}")
    try:
        resp = requests.get(f"{APP1_URL}/api/posts", headers=headers)
        if resp.status_code != 200:
            print(f"{RED}Failed to fetch posts from App1 (Port 8081). Status: {resp.status_code}{NC}")
            sys.exit(1)
        
        posts = resp.json()
        if not posts:
            print(f"{RED}No videos found on the server. Please upload a video first.{NC}")
            sys.exit(1)
            
        # Select the first video
        target_video = posts[0]
        video_path = target_video.get("videoPath")
        if not video_path:
            print(f"{RED}Error: Post data does not contain 'videoPath'.{NC}")
            sys.exit(1)
            
        # Extract name without extension for the API
        video_name = os.path.basename(video_path).replace(".mp4", "")
        print(f"{GREEN}→ Target video: {video_name}{NC}")
        
    except Exception as e:
        print(f"{RED}Error connecting to server: {e}{NC}")
        print(f"{YELLOW}Make sure the Docker containers are running (ports 8081 and 8082).{NC}")
        sys.exit(1)

    # Function to get views for a video from a specific URL
    def get_count(base_url, name):
        try:
            r = requests.get(f"{base_url}/api/posts/{name}", headers=headers)
            if r.status_code == 200:
                return r.json().get("viewsCount", 0)
            return None
        except Exception:
            return None

    # Step 2: Record initial counts
    print(f"\n{BLUE}[Step 2] Recording initial view counts...{NC}")
    v1_initial = get_count(APP1_URL, video_name)
    v2_initial = get_count(APP2_URL, video_name)
    
    print(f"  App1 (8081): {v1_initial} views")
    print(f"  App2 (8082): {v2_initial} views")
    
    if v1_initial is None or v2_initial is None:
        print(f"{RED}Failed to retrieve initial counts. Aborting.{NC}")
        sys.exit(1)

    # Step 3: Increment view on App1
    print(f"\n{BLUE}[Step 3] Incrementing view on App1 (Port 8081)...{NC}")
    try:
        inc_resp = requests.post(f"{APP1_URL}/api/posts/{video_name}/view", headers=headers)
        if inc_resp.status_code == 200:
            print(f"{GREEN}✓ View incremented successfully.{NC}")
        else:
            print(f"{RED}✗ Failed to increment view: {inc_resp.status_code} {inc_resp.text}{NC}")
            sys.exit(1)
    except Exception as e:
        print(f"{RED}Request error: {e}{NC}")
        sys.exit(1)

    # Step 4: Wait 15 seconds as requested
    print(f"\n{BLUE}[Step 4] Waiting 15 seconds for synchronization...{NC}")
    for i in range(15, 0, -1):
        print(f"  Counting down: {i}s  ", end="\r")
        time.sleep(1)
    print("  Sync wait complete!      ")

    # Step 5: Verify results
    print(f"\n{BLUE}[Step 5] Verifying synchronization results...{NC}")
    v1_final = get_count(APP1_URL, video_name)
    v2_final = get_count(APP2_URL, video_name)
    
    print(f"  App1 (8081) final: {v1_final} views")
    print(f"  App2 (8082) final: {v2_final} views")
    
    print(f"\n{BLUE}--- Conclusion ---{NC}")
    
    passed = True
    
    if v1_final > v1_initial:
        print(f"{GREEN}✓ App1 correctly tracked the local view.{NC}")
    else:
        print(f"{RED}✗ App1 did not increment view locally.{NC}")
        passed = False
        
    if v2_final > v2_initial:
        print(f"{GREEN}✓ App2 received the view update via sync.{NC}")
    else:
        print(f"{RED}✗ App2 did not receive the view update (Sync Failure).{NC}")
        passed = False
        
    if passed:
        print(f"\n{GREEN}✅ TEST PASSED: View counting and synchronization are working correctly!{NC}")
    else:
        print(f"\n{RED}❌ TEST FAILED: View synchronization issue detected.{NC}")
        sys.exit(1)

if __name__ == "__main__":
    main()
