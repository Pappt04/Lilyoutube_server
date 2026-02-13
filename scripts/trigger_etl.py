#!/usr/bin/env python3
"""
ETL Pipeline Manual Trigger Script

This script allows manual triggering of the ETL pipeline for popular videos.
It authenticates with the server and calls the trigger endpoint.

Usage:
    python trigger_etl.py --email user@example.com --password mypassword
    python trigger_etl.py --token <existing_token>
    python trigger_etl.py --config config.json
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime
from typing import Optional, Dict, Any

try:
    import requests
except ImportError:
    print("Error: 'requests' library not found. Install it with:")
    print("  pip install requests")
    sys.exit(1)


class ETLTrigger:
    def __init__(self, base_url: str = "http://localhost:8888"):
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.token: Optional[str] = None

    def login(self, email: str, password: str) -> bool:
        """Authenticate and get access token"""
        url = f"{self.base_url}/api/auth/login"
        payload = {
            "email": email,
            "password": password
        }
        
        print(f"🔐 Authenticating as {email}...")
        try:
            response = self.session.post(url, json=payload, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                self.token = data.get('token')
                if self.token:
                    print("✅ Authentication successful!")
                    return True
                else:
                    print("❌ No token in response")
                    return False
            else:
                print(f"❌ Authentication failed: {response.status_code}")
                print(f"   Response: {response.text}")
                return False
                
        except requests.exceptions.RequestException as e:
            print(f"❌ Connection error: {e}")
            return False

    def trigger_etl(self) -> Optional[Dict[str, Any]]:
        """Trigger the ETL pipeline"""
        url = f"{self.base_url}/api/popular-videos/trigger-etl"
        headers = {}

        # Add auth header if token is available (optional)
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        
        print("\n🚀 Triggering ETL pipeline...")
        print(f"   Target: {url}")
        print(f"   Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        start_time = time.time()
        
        try:
            response = self.session.post(url, headers=headers, timeout=120)
            
            elapsed = time.time() - start_time
            
            if response.status_code == 200:
                data = response.json()
                print(f"✅ ETL pipeline completed successfully!")
                print(f"   Local execution time: {elapsed:.2f}s")
                return data
            else:
                print(f"❌ ETL trigger failed: {response.status_code}")
                print(f"   Response: {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            print(f"⏱️  Request timed out (this is normal for long-running ETL)")
            print(f"   The ETL may still be running on the server.")
            return None
        except requests.exceptions.RequestException as e:
            print(f"❌ Connection error: {e}")
            return None

    def get_stats(self) -> Optional[Dict[str, Any]]:
        """Get ETL statistics"""
        url = f"{self.base_url}/api/popular-videos/stats"
        
        try:
            response = self.session.get(url, timeout=10)
            if response.status_code == 200:
                return response.json()
            return None
        except requests.exceptions.RequestException:
            return None

    def get_popular_videos(self, limit: int = 10) -> Optional[list]:
        """Get latest popular videos"""
        url = f"{self.base_url}/api/popular-videos"
        params = {"limit": limit}
        
        try:
            response = self.session.get(url, params=params, timeout=10)
            if response.status_code == 200:
                return response.json()
            return None
        except requests.exceptions.RequestException:
            return None

    def display_results(self, result: Dict[str, Any]):
        """Display ETL execution results"""
        print("\n" + "="*60)
        print("📊 ETL EXECUTION RESULTS")
        print("="*60)
        
        print(f"Status:      {result.get('status', 'unknown')}")
        print(f"Message:     {result.get('message', 'N/A')}")
        print(f"Duration:    {result.get('duration_ms', 0)} ms")
        print(f"Timestamp:   {result.get('timestamp', 'N/A')}")
        
        # Get and display stats
        stats = self.get_stats()
        if stats:
            print("\n" + "-"*60)
            print("📈 ETL STATISTICS")
            print("-"*60)
            print(f"Total Runs:        {stats.get('totalRuns', 0)}")
            print(f"Last Run Time:     {stats.get('lastRunTime', 'N/A')}")
            print(f"Popular Videos:    {stats.get('popularVideoCount', 0)}")
        
        # Get and display popular videos
        videos = self.get_popular_videos(5)
        if videos:
            print("\n" + "-"*60)
            print("🏆 TOP 5 POPULAR VIDEOS")
            print("-"*60)
            for i, video in enumerate(videos[:5], 1):
                post = video.get('post', {})
                score = video.get('score', 0)
                rank = video.get('rank', i)
                title = post.get('title', 'Unknown')
                views = post.get('viewsCount', 0)
                likes = post.get('likesCount', 0)
                comments = post.get('commentsCount', 0)
                
                print(f"\n#{rank}. {title}")
                print(f"    Score: {score:.2f} | Views: {views} | Likes: {likes} | Comments: {comments}")
        
        print("\n" + "="*60)


def load_config(config_file: str) -> Optional[Dict[str, str]]:
    """Load configuration from JSON file"""
    try:
        with open(config_file, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"❌ Config file not found: {config_file}")
        return None
    except json.JSONDecodeError:
        print(f"❌ Invalid JSON in config file: {config_file}")
        return None


def main():
    parser = argparse.ArgumentParser(
        description="Manually trigger ETL pipeline for popular videos",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Using email and password
  python trigger_etl.py --email admin@example.com --password secret123
  
  # Using existing token
  python trigger_etl.py --token eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  
  # Using config file
  python trigger_etl.py --config config.json
  
  # Custom server URL
  python trigger_etl.py --url http://app1:8080 --email user@example.com --password pass
  
Config file format (config.json):
  {
    "email": "admin@example.com",
    "password": "secret123",
    "base_url": "http://localhost:8080"
  }
        """
    )
    
    parser.add_argument('--email', '-e', help='User email for authentication')
    parser.add_argument('--password', '-p', help='User password')
    parser.add_argument('--token', '-t', help='Existing authentication token')
    parser.add_argument('--config', '-c', help='Path to JSON config file')
    parser.add_argument('--url', '-u', default='http://localhost:8888',
                       help='Base URL of the server (default: http://localhost:8888)')
    parser.add_argument('--no-display', action='store_true',
                       help='Skip displaying results (just trigger)')
    
    args = parser.parse_args()
    
    # Load config if provided
    config = {}
    if args.config:
        config = load_config(args.config) or {}
    
    # Get credentials
    email = args.email or config.get('email') or os.environ.get('LILYOUTUBE_EMAIL')
    password = args.password or config.get('password') or os.environ.get('LILYOUTUBE_PASSWORD')
    token = args.token or config.get('token') or os.environ.get('LILYOUTUBE_TOKEN')
    base_url = args.url or config.get('base_url', 'http://localhost:8888')
    
    # Create ETL trigger
    etl = ETLTrigger(base_url)

    # Authenticate if credentials provided (optional)
    if token:
        print(f"🔑 Using provided token")
        etl.token = token
    elif email and password:
        if not etl.login(email, password):
            print("⚠️  Authentication failed, but continuing without auth...")
    else:
        print("ℹ️  No credentials provided - triggering ETL without authentication")
    
    # Trigger ETL
    result = etl.trigger_etl()
    
    if result:
        if not args.no_display:
            etl.display_results(result)
        print("\n✅ ETL trigger completed successfully!")
        sys.exit(0)
    else:
        print("\n❌ ETL trigger failed!")
        sys.exit(1)


if __name__ == "__main__":
    main()
