import requests
import json
import time
import concurrent.futures
import sys

# Configuration
BASE_URL = "http://localhost:8080/api/posts"
POST_ID = "6aaa6bac-4d38-4d36-a818-a88c3d16fb4c"
BEARER_TOKEN = "993af60a-964b-4b25-9a51-a3425ed5391a"
NUM_REQUESTS = 100

# API endpoints
GET_URL = f"{BASE_URL}/{POST_ID}"
VIEW_URL = f"{BASE_URL}/{POST_ID}/view"

# Colors for output
RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m' # No Color

def print_header():
    print("============================================================")
    print("Concurrency Test - Video View Counter")
    print("============================================================")
    print(f"Target: {VIEW_URL}")
    print(f"Number of requests: {NUM_REQUESTS}")
    print(f"Time: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("============================================================")
    print("")

def get_view_count():
    headers = {"Authorization": f'Bearer {{"token":"{BEARER_TOKEN}"}}'}
    try:
        response = requests.get(GET_URL, headers=headers)
        if response.status_code != 200:
            print(f"{RED}ERROR: HTTP {response.status_code} while fetching view count{NC}", file=sys.stderr)
            return None
        
        data = response.json()
        return data.get("viewsCount")
    except Exception as e:
        print(f"{RED}ERROR: {e}{NC}", file=sys.stderr)
        return None

def send_view_request(request_id):
    headers = {
        "Authorization": f'Bearer {{"token":"{BEARER_TOKEN}"}}',
        "Content-Type": "application/json"
    }
    try:
        response = requests.post(VIEW_URL, headers=headers)
        if response.status_code in [200, 201, 204]:
            return "OK"
        else:
            return f"FAIL:{response.status_code}"
    except Exception as e:
        return f"ERROR:{e}"

def run_test():
    print_header()
    
    # Step 1: Get initial view count
    print("Step 1: Fetching initial view count...")
    initial_count = get_view_count()
    if initial_count is None:
        print(f"{RED}❌ Failed to fetch initial view count. Exiting.{NC}")
        sys.exit(1)
    
    print(f"{GREEN}✓{NC} Initial view count: {initial_count}")
    print("")
    
    # Step 2: Send concurrent requests
    print(f"Step 2: Sending {NUM_REQUESTS} concurrent POST requests...")
    
    start_time = time.time()
    
    successful = 0
    failed = 0
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=NUM_REQUESTS) as executor:
        futures = [executor.submit(send_view_request, i) for i in range(NUM_REQUESTS)]
        for future in concurrent.futures.as_completed(futures):
            result = future.result()
            if result == "OK":
                successful += 1
            else:
                failed += 1
    
    end_time = time.time()
    duration = end_time - start_time
    
    print(f"{GREEN}✓{NC} Completed in {duration:.4f} seconds")
    print(f"  - Successful: {successful}")
    print(f"  - Failed: {failed}")
    print("")
    
    # Step 3: Wait for server to process
    print("Step 3: Waiting for server to process requests...")
    time.sleep(2)
    print("")
    
    # Step 4: Get final view count
    print("Step 4: Fetching final view count...")
    final_count = get_view_count()
    if final_count is None:
        print(f"{RED}❌ Failed to fetch final view count. Exiting.{NC}")
        sys.exit(1)
    
    print(f"{GREEN}✓{NC} Final view count: {final_count}")
    print("")
    
    # Step 5: Verify results
    print("============================================================")
    print("RESULTS")
    print("============================================================")
    print(f"Initial count:     {initial_count}")
    print(f"Final count:       {final_count}")
    print(f"Expected increase: {NUM_REQUESTS}")
    
    actual_increase = final_count - initial_count
    print(f"Actual increase:   {actual_increase}")
    print("============================================================")
    print("")
    
    expected_final = initial_count + NUM_REQUESTS
    
    if final_count == expected_final:
        print(f"{GREEN}✅ TEST PASSED: View count increased by exactly {NUM_REQUESTS}!{NC}")
        print(f"{GREEN}   The system handles concurrency correctly.{NC}")
    else:
        difference = expected_final - final_count
        print(f"{RED}❌ TEST FAILED: View count mismatch!{NC}")
        print(f"   Expected final count: {expected_final}")
        print(f"   Actual final count:   {final_count}")
        print(f"   Difference:           {difference} views")
        print("")
        
        if difference > 0:
            print(f"   {YELLOW}→ {difference} views were LOST (race condition detected){NC}")
        else:
            print(f"   {YELLOW}→ {abs(difference)} EXTRA views were counted{NC}")
        sys.exit(1)

if __name__ == "__main__":
    run_test()
