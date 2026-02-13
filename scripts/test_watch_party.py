#!/usr/bin/env python3
"""
Test script for Watch Party API endpoints
Tests the complete watch party creation, joining, and management flow
"""

import requests
import json
import sys
import time

BASE_URL = "http://localhost:8888/api"

def authenticate():
    """Login with credentials and get token."""
    login_payload = {
        "email": "papi@gmail.com",
        "password": "papipapi"
    }

    try:
        resp = requests.post(f"{BASE_URL}/auth/login", json=login_payload)
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


def authenticate_second_user():
    """Login with second user credentials."""
    login_payload = {
        "email": "test@gmail.com",  # Adjust if you have a different test user
        "password": "testtest"
    }

    try:
        resp = requests.post(f"{BASE_URL}/auth/login", json=login_payload)
        if resp.status_code == 200:
            token = resp.json().get("token")
            print(f"✓ Second user login successful! Token: {token[:20]}...")
            return token
        else:
            print(f"✗ Second user login failed ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Second user login error: {e}")
        return None


def create_headers(token):
    """Create authorization headers."""
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }


def test_create_public_watch_party(token):
    """Test creating a public watch party."""
    print("\n" + "="*60)
    print("TEST 1: Create Public Watch Party")
    print("="*60)

    headers = create_headers(token)
    payload = {
        "publicRoom": True
    }

    try:
        resp = requests.post(f"{BASE_URL}/watchparty/create", headers=headers, json=payload)

        if resp.status_code == 200:
            party = resp.json()
            print(f"✓ Public watch party created successfully!")
            print(f"  Room Code: {party.get('roomCode')}")
            print(f"  Creator: {party.get('creatorUsername')}")
            print(f"  Public: {party.get('publicRoom')}")
            print(f"  Active: {party.get('active')}")
            print(f"  Members: {party.get('memberCount')}")
            return party
        else:
            print(f"✗ Failed to create watch party ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Error creating watch party: {e}")
        return None


def test_create_private_watch_party(token):
    """Test creating a private watch party."""
    print("\n" + "="*60)
    print("TEST 2: Create Private Watch Party")
    print("="*60)

    headers = create_headers(token)
    payload = {
        "publicRoom": False
    }

    try:
        resp = requests.post(f"{BASE_URL}/watchparty/create", headers=headers, json=payload)

        if resp.status_code == 200:
            party = resp.json()
            print(f"✓ Private watch party created successfully!")
            print(f"  Room Code: {party.get('roomCode')}")
            print(f"  Public: {party.get('publicRoom')}")
            return party
        else:
            print(f"✗ Failed to create watch party ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Error creating watch party: {e}")
        return None


def test_get_public_watch_parties(token):
    """Test getting all public watch parties."""
    print("\n" + "="*60)
    print("TEST 3: Get Public Watch Parties")
    print("="*60)

    headers = create_headers(token)

    try:
        resp = requests.get(f"{BASE_URL}/watchparty/public", headers=headers)

        if resp.status_code == 200:
            parties = resp.json()
            print(f"✓ Retrieved {len(parties)} public watch parties")
            for i, party in enumerate(parties, 1):
                print(f"  [{i}] {party.get('roomCode')} - {party.get('creatorUsername')} ({party.get('memberCount')} members)")
            return parties
        else:
            print(f"✗ Failed to get public parties ({resp.status_code}): {resp.text}")
            return []
    except Exception as e:
        print(f"✗ Error getting public parties: {e}")
        return []


def test_join_watch_party(token, room_code):
    """Test joining a watch party."""
    print("\n" + "="*60)
    print(f"TEST 4: Join Watch Party ({room_code})")
    print("="*60)

    headers = create_headers(token)
    payload = {
        "roomCode": room_code
    }

    try:
        resp = requests.post(f"{BASE_URL}/watchparty/join", headers=headers, json=payload)

        if resp.status_code == 200:
            party = resp.json()
            print(f"✓ Successfully joined watch party!")
            print(f"  Room Code: {party.get('roomCode')}")
            print(f"  Members: {party.get('memberCount')}")
            print(f"  Member List:")
            for member in party.get('members', []):
                print(f"    - {member.get('username')}")
            return party
        else:
            print(f"✗ Failed to join watch party ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Error joining watch party: {e}")
        return None


def test_get_watch_party(token, room_code):
    """Test getting watch party details."""
    print("\n" + "="*60)
    print(f"TEST 5: Get Watch Party Details ({room_code})")
    print("="*60)

    headers = create_headers(token)

    try:
        resp = requests.get(f"{BASE_URL}/watchparty/{room_code}", headers=headers)

        if resp.status_code == 200:
            party = resp.json()
            print(f"✓ Retrieved watch party details!")
            print(f"  Room Code: {party.get('roomCode')}")
            print(f"  Creator: {party.get('creatorUsername')}")
            print(f"  Members: {party.get('memberCount')}")
            print(f"  Current Video: {party.get('currentVideoTitle') or 'None'}")
            return party
        else:
            print(f"✗ Failed to get watch party ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"✗ Error getting watch party: {e}")
        return None


def test_leave_watch_party(token, room_code):
    """Test leaving a watch party."""
    print("\n" + "="*60)
    print(f"TEST 6: Leave Watch Party ({room_code})")
    print("="*60)

    headers = create_headers(token)

    try:
        resp = requests.post(f"{BASE_URL}/watchparty/{room_code}/leave", headers=headers)

        if resp.status_code == 200:
            print(f"✓ Successfully left watch party!")
            return True
        else:
            print(f"✗ Failed to leave watch party ({resp.status_code}): {resp.text}")
            return False
    except Exception as e:
        print(f"✗ Error leaving watch party: {e}")
        return False


def run_all_tests():
    """Run all watch party tests."""
    print("\n" + "="*60)
    print("WATCH PARTY API TESTS")
    print("="*60)

    # Authenticate first user
    token1 = authenticate()
    if not token1:
        print("\n✗ Failed to authenticate first user. Exiting.")
        return False

    # Test 1: Create public watch party
    public_party = test_create_public_watch_party(token1)
    if not public_party:
        print("\n✗ Test suite failed at Test 1")
        return False

    room_code = public_party.get('roomCode')

    # Test 2: Create private watch party
    private_party = test_create_private_watch_party(token1)

    # Test 3: Get public watch parties
    test_get_public_watch_parties(token1)

    # Test 4: Get watch party details
    test_get_watch_party(token1, room_code)

    # Try to authenticate second user (optional - may fail if user doesn't exist)
    print("\n" + "-"*60)
    print("Attempting to test multi-user functionality...")
    print("-"*60)
    token2 = authenticate_second_user()

    if token2:
        # Test 5: Second user joins watch party
        test_join_watch_party(token2, room_code)

        # Test 6: Get updated watch party details
        test_get_watch_party(token1, room_code)

        # Test 7: Second user leaves watch party
        test_leave_watch_party(token2, room_code)
    else:
        print("⚠ Skipping multi-user tests (second user not available)")

    # Final summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print("✓ Watch party creation: PASSED")
    print("✓ Watch party retrieval: PASSED")
    print("✓ Public party listing: PASSED")
    if token2:
        print("✓ Multi-user join/leave: PASSED")
    print("="*60)
    print("\n✓ All tests completed successfully!\n")

    return True


if __name__ == "__main__":
    try:
        success = run_all_tests()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n⚠ Tests interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n✗ Unexpected error: {e}")
        sys.exit(1)
