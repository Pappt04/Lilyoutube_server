import requests
import json
import time
import os
import uuid

BASE_URL_1 = "http://localhost:8081"
BASE_URL_2 = "http://localhost:8082"
GATEWAY_URL = "http://localhost:8888"

FALLBACK_TOKEN = "1db0abb2-77dc-4171-acbc-7222954d6b4a"

def authenticate():
    print("[2] Authenticating...")
    
    login_payload = {
        "email": "mock@gmail.com",
        "password": "password"
    }
    
    try:
        resp = requests.post(f"{GATEWAY_URL}/api/auth/login", json=login_payload)
        if resp.status_code == 200:
            token = resp.json().get("token")
            print("    Login successful! Token acquired.")
            return token
        else:
             print(f"    Login failed ({resp.status_code}). Attempting Register...")
    except Exception as e:
        print(f"    Login error: {e}")
        return None

    unique_suffix = str(uuid.uuid4())[:8]
    register_payload = {
        "email": f"mock_{unique_suffix}@gmail.com",
        "username": f"user_{unique_suffix}",
        "password": "password",
        "confirmPassword": "password",
        "firstName": "Test",
        "lastName": "User",
        "address": "Test Address"
    }

    try:
        print(f"    Registering as {register_payload['email']}...")
        resp = requests.post(f"{GATEWAY_URL}/api/auth/register", json=register_payload)
        if resp.status_code == 200:
            print("    Registration initiated. Check likely email failure in logs.")
        else:
            print(f"    Registration failed: {resp.status_code} - {resp.text}")
            
    except Exception as e:
        print(f"    Register error: {e}")

    return None

def demo_sync():
    print("==========================================")
    print(" Lilyoutube Replica Sync Demo (Python)")
    print("==========================================")

    print("\n[1] Creating dummy files...")
    with open("dummy.mp4", "w") as f:
        f.write("dummy video content")
    with open("dummy.jpg", "w") as f:
        f.write("dummy thumbnail content")

    token = authenticate()
    if not token:
        print("CRITICAL: Authentication failed. Cannot proceed.")
        print("Tip: Ensure the database has a valid user or email service is configured.")
        return

    headers = {
        "Authorization": f"Bearer {{\"token\": \"{token}\"}}"
    }

    print("[3] Uploading video...")
    post_metadata = {
        "title": "Sync Benchmark Video",
        "description": "Testing sync",
        "tags": ["test", "sync"]
    }
    
    files = {
        'post': (None, json.dumps(post_metadata), 'application/json'),
        'video': ('dummy.mp4', open('dummy.mp4', 'rb'), 'video/mp4'),
        'thumbnail': ('dummy.jpg', open('dummy.jpg', 'rb'), 'image/jpeg')
    }

    video_name = None
    try:
        resp = requests.post(f"{GATEWAY_URL}/api/posts", headers=headers, files=files)
        if resp.status_code == 200:
            video_data = resp.json()
            video_path = video_data.get("videoPath")
            video_name = video_path.replace(".mp4", "")
            print(f"    Video uploaded! ID: {video_name}")
        else:
             print(f"    Upload failed ({resp.status_code}): {resp.text}")
             return
    except Exception as e:
        print(f"    Upload failed: {e}")
        return
    finally:
        pass

    if not video_name:
        return

    print(f"\n[4] Sending views...")
    print("    Sending 15 views to App1...", end="", flush=True)
    for _ in range(15):
        try:
             requests.post(f"{BASE_URL_1}/api/posts/{video_name}/view", headers=headers, timeout=2)
             print(".", end="", flush=True)
        except:
             print("x", end="", flush=True)
    print(" Done.")

    print("    Sending 10 views to App2...", end="", flush=True)
    for _ in range(10):
        try:
            requests.post(f"{BASE_URL_2}/api/posts/{video_name}/view", headers=headers, timeout=2)
            print(".", end="", flush=True)
        except:
            print("x", end="", flush=True)
    print(" Done.\n")

    print("[5] Observing state...")
    
    def get_state(url, app_name):
        try:
            resp = requests.get(f"{url}/api/posts/views/replica-table", headers=headers, timeout=2)
            if resp.status_code == 200:
                data = resp.json()
                video_row = [row for row in data if row.get("videoName") == f"{video_name}.mp4"]
                print(f"    {app_name} sees:")
                if video_row:
                    for row in video_row:
                        print(f"      - Replica: {row.get('replicaName')}, Views: {row.get('views')}")
                else:
                    print("      No views yet.")
            else:
                 print(f"    {app_name} returned {resp.status_code}")
        except Exception as e:
            print(f"    Error connecting to {app_name}: {e}")

    print("--- Immediate Check ---")
    get_state(BASE_URL_1, "APP 1")
    get_state(BASE_URL_2, "APP 2")

    print(f"\n    Waiting 15 seconds for sync...")
    time.sleep(15)

    print("--- Check AFTER Sync ---")
    get_state(BASE_URL_1, "APP 1")
    get_state(BASE_URL_2, "APP 2")

    # Cleanup
    if os.path.exists("dummy.mp4"):
        os.remove("dummy.mp4")
    if os.path.exists("dummy.jpg"):
        os.remove("dummy.jpg")

if __name__ == "__main__":
    demo_sync()
