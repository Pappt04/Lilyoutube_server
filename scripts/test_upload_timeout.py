import subprocess
import time
import os
import sys

URL = "http://localhost:8080/api/posts"
TOKEN = "993af60a-964b-4b25-9a51-a3425ed5391a"
VIDEO_FILE = "/home/papp/Videos/menzans_backgroundlocation.mp4"
THUMB_FILE = "/home/papp/Pictures/ftnsebudi.jpg"
POST_DATA = '{"user_id":1, "title":"Test Video", "description":"Testing timeout", "tags":["test"]}'
TIMEOUT_THRESHOLD = 60
UPLOAD_RATE_SLOW = "20k"
UPLOAD_RATE_FAST = "5000k"

def create_test_files():
    video_size = int(2.5 * 1024 * 1024)
    thumb_size = 100 * 1024

    os.makedirs(os.path.dirname(VIDEO_FILE) or '/tmp', exist_ok=True)
    with open(VIDEO_FILE, "wb") as f:
        f.write(os.urandom(video_size))
    with open(THUMB_FILE, "wb") as f:
        f.write(os.urandom(thumb_size))

def test_upload(rate_limit, expected_to_timeout=False):
    test_name = "slow_upload" if expected_to_timeout else "fast_upload"
    print(f"Testing {test_name} (rate={rate_limit})...")

    start_time = time.time()

    curl_command = [
        "curl", "-s", "-w", "\\nHTTP_CODE:%{http_code}",
        "-X", "POST", URL,
        "-H", f'Authorization: Bearer {{"token":"{TOKEN}"}}',
        "--limit-rate", rate_limit,
        "--max-time", str(TIMEOUT_THRESHOLD + 10),
        "-F", f"post={POST_DATA}",
        "-F", f"video=@{VIDEO_FILE}",
        "-F", f"thumbnail=@{THUMB_FILE}"
    ]

    try:
        result = subprocess.run(curl_command, capture_output=True, text=True, timeout=TIMEOUT_THRESHOLD + 20)

        http_code = "UNKNOWN"
        for line in result.stdout.split('\n'):
            if line.startswith('HTTP_CODE:'):
                http_code = line.split(':')[1].strip()

        elapsed = time.time() - start_time
        connection_error = any(err in result.stderr.lower() for err in ['timeout', 'connection', 'broken pipe', 'reset', 'empty reply'])

        print(f"  http_code={http_code}, elapsed={elapsed:.1f}s, conn_error={connection_error}")

        if expected_to_timeout:
            passed = http_code in ['408', '500', '504', '000'] or connection_error
        else:
            passed = http_code in ['200', '201']

        print(f"  {'PASS' if passed else 'FAIL'}")
        return passed

    except subprocess.TimeoutExpired:
        print(f"  Process timeout after {TIMEOUT_THRESHOLD + 20}s")
        return expected_to_timeout
    except Exception as e:
        print(f"  Error: {e}")
        return False

def cleanup():
    for f in [VIDEO_FILE, THUMB_FILE]:
        if os.path.exists(f):
            os.remove(f)

def main():
    create_test_files()

    test1_passed = test_upload(UPLOAD_RATE_FAST, expected_to_timeout=False)
    test2_passed = test_upload(UPLOAD_RATE_SLOW, expected_to_timeout=True)

    cleanup()

    print(f"\nResults: fast={'PASS' if test1_passed else 'FAIL'}, slow={'PASS' if test2_passed else 'FAIL'}")

    sys.exit(0 if (test1_passed and test2_passed) else 1)

if __name__ == "__main__":
    main()