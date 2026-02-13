#!/usr/bin/env python3
"""
Test script that sends 50 messages to test the implementation.
This script posts 50 comments to verify system behavior under load.
"""

import requests
import json
import sys
import time
from datetime import datetime

def authenticate():
    """Login with credentials and get token."""
    login_payload = {
        "email": "papi@gmail.com",
        "password": "papipapi"
    }

    try:
        resp = requests.post("http://localhost:8888/api/auth/login", json=login_payload)
        if resp.status_code == 200:
            token = resp.json().get("token")
            print(f"✓ Login successful! Token: {token[:20]}...")
            return token
        else:
            print(f"✗ Login failed ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Login error: {e}")
        return None

def test_50_messages():
    """Send 50 messages to test the implementation."""
    # Configuration
    BASE_URL = "http://localhost:8080"
    POST_ID = 2
    USER_ID = 2
    TEXT_PREFIX = "Test message"
    NUM_MESSAGES = 50

    # Authenticate first
    TOKEN = authenticate()
    if not TOKEN:
        print("\nERROR: Failed to authenticate. Exiting.")
        sys.exit(1)

    headers = {
        "Authorization": f"Bearer {{\"token\":\"{TOKEN}\"}}",
        "Content-Type": "application/json"
    }

    comment_url = f"{BASE_URL}/api/comments"

    print("\n" + "="*60)
    print(f"Testing with {NUM_MESSAGES} messages")
    print(f"Target URL: {comment_url}")
    print(f"Post ID: {POST_ID}, User ID: {USER_ID}")
    print("="*60 + "\n")

    # Statistics
    stats = {
        "success": 0,
        "failed": 0,
        "rate_limited": 0,
        "errors": 0,
        "total_time": 0
    }

    start_time = time.time()

    # Send 50 messages
    for i in range(1, NUM_MESSAGES + 1):
        payload = {
            "user_id": USER_ID,
            "post_id": POST_ID,
            "text": f"{TEXT_PREFIX} {i} at {datetime.now().strftime('%H:%M:%S.%f')[:-3]}"
        }

        request_start = time.time()

        try:
            resp = requests.post(comment_url, headers=headers, json=payload, timeout=10)
            request_time = time.time() - request_start

            if resp.status_code == 200:
                stats["success"] += 1
                print(f"[{i:2d}/50] ✓ Success (took {request_time:.3f}s)")
            elif resp.status_code == 429:
                stats["rate_limited"] += 1
                print(f"[{i:2d}/50] ⚠ Rate Limited: {resp.text}")
            elif resp.status_code == 400:
                stats["failed"] += 1
                print(f"[{i:2d}/50] ✗ Bad Request: {resp.text}")
            elif resp.status_code == 401:
                stats["failed"] += 1
                print(f"[{i:2d}/50] ✗ Unauthorized: {resp.text}")
            elif resp.status_code == 500:
                stats["errors"] += 1
                print(f"[{i:2d}/50] ✗ Server Error: {resp.text}")
            else:
                stats["failed"] += 1
                print(f"[{i:2d}/50] ✗ Unexpected status {resp.status_code}: {resp.text}")

        except requests.exceptions.Timeout:
            stats["errors"] += 1
            print(f"[{i:2d}/50] ✗ Request timeout")
        except Exception as e:
            stats["errors"] += 1
            print(f"[{i:2d}/50] ✗ Request failed: {e}")

        # Small delay between requests to avoid overwhelming the server
        time.sleep(0.1)

    stats["total_time"] = time.time() - start_time

    # Print summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print(f"Total Messages Sent:  {NUM_MESSAGES}")
    print(f"Successful:           {stats['success']} ({stats['success']/NUM_MESSAGES*100:.1f}%)")
    print(f"Rate Limited:         {stats['rate_limited']} ({stats['rate_limited']/NUM_MESSAGES*100:.1f}%)")
    print(f"Failed:               {stats['failed']} ({stats['failed']/NUM_MESSAGES*100:.1f}%)")
    print(f"Errors:               {stats['errors']} ({stats['errors']/NUM_MESSAGES*100:.1f}%)")
    print(f"Total Time:           {stats['total_time']:.2f}s")
    print(f"Average Time/Message: {stats['total_time']/NUM_MESSAGES:.3f}s")
    print(f"Messages/Second:      {NUM_MESSAGES/stats['total_time']:.2f}")
    print("="*60)

    # Return exit code based on success rate
    if stats['success'] >= NUM_MESSAGES * 0.8:  # 80% success rate
        print("\n✓ Test PASSED (≥80% success rate)")
        return 0
    else:
        print("\n✗ Test FAILED (<80% success rate)")
        return 1

if __name__ == "__main__":
    exit_code = test_50_messages()
    sys.exit(exit_code)
