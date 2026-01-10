import subprocess
import time
import os
import sys

# Configuration
URL = "http://localhost:8080/api/posts"
TOKEN = "993af60a-964b-4b25-9a51-a3425ed5391a"
VIDEO_FILE = "/home/papp/Videos/menzans_backgroundlocation.mp4"
THUMB_FILE = "/home/papp/Pictures/ftnsebudi.jpg"
POST_DATA = '{"user_id":1, "title":"Test Video", "description":"Testing timeout", "tags":["test"]}'
MAX_TIME = 60 # seconds
UPLOAD_RATE = "30k" # 30 KB/s - should make 2MB take > 60s

def ensure_files_exist():
    # We need a file large enough that at UPLOAD_RATE it takes > 60s
    # 30 KB/s * 65s = 1.95 MB. Let's make it 2.5 MB.
    file_size = 2.5 * 1024 * 1024 
    if not os.path.exists(VIDEO_FILE) or os.path.getsize(VIDEO_FILE) < file_size:
        os.makedirs(os.path.dirname(VIDEO_FILE), exist_ok=True)
        with open(VIDEO_FILE, "wb") as f:
            f.write(os.urandom(int(file_size)))
    if not os.path.exists(THUMB_FILE):
        os.makedirs(os.path.dirname(THUMB_FILE), exist_ok=True)
        with open(THUMB_FILE, "wb") as f:
            f.write(os.urandom(512 * 1024))

def test_upload():
    ensure_files_exist()
    
    print(f"Testing SLOW upload to {URL} using curl throttling...")
    print(f"Using Token: {TOKEN}")
    print(f"Rate Limit: {UPLOAD_RATE}B/s")
    
    start_time = time.time()
    
    curl_command = [
        "curl", "-s", "-w", "%{http_code}",
        "-X", "POST", URL,
        "-H", f'Authorization: Bearer {{"token":"{TOKEN}"}}',
        "--limit-rate", UPLOAD_RATE,
        "-F", f"post={POST_DATA}",
        "-F", f"video=@{VIDEO_FILE}",
        "-F", f"thumbnail=@{THUMB_FILE}",
        "-o", "/dev/null"
    ]
    
    try:
        process = subprocess.run(curl_command, capture_output=True, text=True)
        status_code = process.stdout.strip()
    except Exception as e:
        print(f"\nUpload failed: {e}")
        status_code = "ERROR"
    
    end_time = time.time()
    elapsed = int(end_time - start_time)
    
    print(f"\nUpload finished with HTTP status: {status_code}")
    print(f"Total time elapsed: {elapsed} seconds")
    
    if elapsed >= MAX_TIME:
        print(f"SUCCESS: The upload successfully took {elapsed} seconds.")
    else:
        print(f"FAILED: The upload finished too quickly ({elapsed}s).")
        sys.exit(1)

    if status_code == "500":
        print("RESULT: Server returned 500. This is likely the timeout logic triggering.")
    elif status_code == "200" or status_code == "201":
        print("RESULT: Server accepted the upload despite the delay. (No timeout triggered)")
    else:
        print(f"RESULT: Server responded with {status_code}.")

if __name__ == "__main__":
    test_upload()
