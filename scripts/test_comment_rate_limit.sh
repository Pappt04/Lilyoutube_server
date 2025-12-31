#!/bin/bash

# Script to test comment rate limiting (60 comments per hour)
# Usage: ./test_comment_rate_limit.sh <post_id> <user_id> <base_url>

POST_ID=${1:-1}
USER_ID=${2:-1}
BASE_URL=${3:-"http://localhost:8080/api"}

echo "Testing comment rate limiting..."
echo "Post ID: $POST_ID"
echo "User ID: $USER_ID"
echo "Base URL: $BASE_URL"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0
RATE_LIMIT_COUNT=0

# Try to send 70 comments (10 more than the limit)
for i in $(seq 1 70); do
    echo -n "Sending comment $i... "
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/comments" \
        -H "Content-Type: application/json" \
        -d "{
            \"post_id\": $POST_ID,
            \"user_id\": $USER_ID,
            \"text\": \"Test comment $i\"
        }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "SUCCESS"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    elif [ "$HTTP_CODE" -eq 429 ]; then
        echo "RATE LIMIT (429)"
        RATE_LIMIT_COUNT=$((RATE_LIMIT_COUNT + 1))
        FAIL_COUNT=$((FAIL_COUNT + 1))
    elif [ "$HTTP_CODE" -eq 401 ]; then
        echo "UNAUTHORIZED (401)"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    else
        echo "FAILED (HTTP $HTTP_CODE)"
        echo "Response: $BODY"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    
    # Small delay to avoid overwhelming the server
    sleep 0.1
done

echo ""
echo "========================================="
echo "Test Results:"
echo "Successful comments: $SUCCESS_COUNT"
echo "Rate limited (429): $RATE_LIMIT_COUNT"
echo "Failed comments: $FAIL_COUNT"
echo "========================================="
echo ""
echo "Expected: First 60 comments should succeed, remaining should be rate limited (429)"

