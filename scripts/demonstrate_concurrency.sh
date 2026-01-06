#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080/api/posts"
POST_ID=452  # Assumes a post with ID 1 exists. Adjust if necessary.
CONCURRENT_REQUESTS=20

echo "Starting concurrency demonstration for Post ID: $POST_ID"

# 1. Get initial view count
echo "Fetching available posts..."
POST_JSON=$(curl -s "$BASE_URL")
#POST_ID=$(echo "$POST_JSON" | grep -oP '"id":\s*\K\d+' | head -n 1)

if [ -z "$POST_ID" ]; then
    echo "No posts found! Please create a post first."
    exit 1
fi

echo "Using Post ID: $POST_ID"

# 1. Get initial view count
INITIAL_VIEWS=$(echo "$POST_JSON" | grep -oP '"id":\s*'"$POST_ID"',.*?"viewsCount":\s*\K\d+' | head -n 1 || echo "0")
echo "Initial views: $INITIAL_VIEWS"

# 2. Launch concurrent requests
echo "Launching $CONCURRENT_REQUESTS concurrent view increments..."
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    curl -s -X POST "$BASE_URL/$POST_ID/view" > /dev/null &
done
wait

echo "All requests finished."

# 4. Get final view count
FINAL_VIEWS=$(curl -s "$BASE_URL" | grep -oP '"id":\s*'"$POST_ID"',.*?"viewsCount":\s*\K\d+' | head -n 1 || echo "0")
echo "Final views: $FINAL_VIEWS"

EXPECTED_VIEWS=$((INITIAL_VIEWS + CONCURRENT_REQUESTS))

if [ "$FINAL_VIEWS" -eq "$EXPECTED_VIEWS" ]; then
    echo "SUCCESS: Concurrency handled correctly. ($FINAL_VIEWS views)"
else
    echo "FAILURE: View count mismatch! Expected $EXPECTED_VIEWS but found $FINAL_VIEWS"
fi
