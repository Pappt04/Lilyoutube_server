import requests
import time
import subprocess
import sys
import os

# Colors for output
GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m'

# Gateway (Nginx) URL
GATEWAY_URL = "http://localhost:8888"

def call_api():
    """Calls the API through the load balancer."""
    try:
        # Calling public posts endpoint
        resp = requests.get(f"{GATEWAY_URL}/api/posts", timeout=5)
        if resp.status_code == 200:
            print(f"{GREEN}  ✓ API call successful. Status: 200. Count: {len(resp.json())} posts.{NC}")
            return True
        else:
            print(f"{RED}  ✗ API call failed. Status: {resp.status_code}{NC}")
            return False
    except Exception as e:
        print(f"{RED}  ✗ API call error: {e}{NC}")
        return False

def run_command(cmd):
    """Runs a shell command."""
    print(f"{YELLOW}Running: {cmd}{NC}")
    process = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if process.returncode != 0:
        print(f"{RED}Command failed: {process.stderr}{NC}")
        return False
    return True

def main():
    print(f"{BLUE}=========================================================={NC}")
    print(f"{BLUE}          Resilience and High Availability Test           {NC}")
    print(f"{BLUE}=========================================================={NC}")

    # Check if gateway is up
    print(f"\n{BLUE}[Test 1] Initial Status Check{NC}")
    if not call_api():
        print(f"{RED}System is not responding at {GATEWAY_URL}. Make sure docker-compose is up.{NC}")
        sys.exit(1)

    # Failure of one replica
    print(f"\n{BLUE}[Test 2] Failure of one replica (app2)...{NC}")
    if not run_command("docker stop lilyoutube_server-app2-1"):
        print(f"{RED}Failed to stop app2.{NC}")
        sys.exit(1)
    
    print(f"{YELLOW}Waiting a few seconds for Nginx to notice...{NC}")
    time.sleep(5)
    
    print(f"{BLUE}Calling API through Load Balancer (should still work via app1)...{NC}")
    if call_api():
        print(f"{GREEN}✅ SUCCESS: System remained functional with one replica down.{NC}")
    else:
        print(f"{RED}❌ FAILURE: System became unresponsive with one replica down.{NC}")

    # Restarting the replica
    print(f"\n{BLUE}[Test 3] Restarting the replica (app2)...{NC}")
    if not run_command("docker start lilyoutube_server-app2-1"):
        print(f"{RED}Failed to start app2.{NC}")
        sys.exit(1)
    
    print(f"{YELLOW}Waiting for replica to become healthy (approx 20s)...{NC}")
    for i in range(25, 0, -1):
        print(f"  Waiting: {i}s   ", end="\r")
        time.sleep(1)
    print("\n")
    
    print(f"{BLUE}Calling API through Load Balancer...{NC}")
    if call_api():
        print(f"{GREEN}✅ SUCCESS: System is fully functional after replica restart.{NC}")
    else:
        print(f"{RED}❌ FAILURE: System did not recover correctly after replica restart.{NC}")

    print(f"\n{BLUE}=========================================================={NC}")
    print(f"{GREEN}         Resilience Test Completed Successfully!          {NC}")
    print(f"{BLUE}=========================================================={NC}")

if __name__ == "__main__":
    main()
