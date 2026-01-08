#!/bin/bash
# Concurrency test script for video view counter
# Tests if 100 concurrent requests result in exactly 100 view increments

set -e

# Configuration
BASE_URL="http://localhost:8080/api/posts"
POST_ID="9ba281b7-b5e7-42ae-a698-d420a7f685d6"
BEARER_TOKEN="32295469-810f-466a-be75-5c32663c689e"
NUM_REQUESTS=100

# API endpoints
GET_URL="${BASE_URL}/${POST_ID}"
VIEW_URL="${BASE_URL}/${POST_ID}/view"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
print_header() {
    echo "============================================================"
    echo "Concurrency Test - Video View Counter"
    echo "============================================================"
    echo "Target: ${VIEW_URL}"
    echo "Number of requests: ${NUM_REQUESTS}"
    echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================================"
    echo ""
}

# Get view count from API
get_view_count() {
    local response=$(curl -s -H "Authorization: Bearer {\"token\":\"${BEARER_TOKEN}\"}" "${GET_URL}")
    local view_count=$(echo "$response" | grep -o '"viewsCount":[0-9]*' | grep -o '[0-9]*')
    
    if [ -z "$view_count" ]; then
        echo "ERROR: Could not parse view count from response" >&2
        echo "$response" >&2
        return 1
    fi
    
    echo "$view_count"
}

# Send a single view request
send_view_request() {
    local request_id=$1
    local status_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Authorization: Bearer {\"token\":\"${BEARER_TOKEN}\"}" \
        -H "Content-Type: application/json" \
        "${VIEW_URL}")
    
    # Consider 200, 201, 204 as success
    if [ "$status_code" -eq 200 ] || [ "$status_code" -eq 201 ] || [ "$status_code" -eq 204 ]; then
        echo "OK"
    else
        echo "FAIL:${status_code}"
    fi
}

# Main test function
run_test() {
    print_header
    
    # Step 1: Get initial view count
    echo "Step 1: Fetching initial view count..."
    INITIAL_COUNT=$(get_view_count)
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to fetch initial view count. Exiting.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓${NC} Initial view count: ${INITIAL_COUNT}"
    echo ""
    
    # Step 2: Send concurrent requests
    echo "Step 2: Sending ${NUM_REQUESTS} concurrent POST requests..."
    
    START_TIME=$(date +%s.%N)
    
    # Launch all requests in parallel
    for i in $(seq 1 $NUM_REQUESTS); do
        send_view_request $i > /tmp/request_${i}.result 2>&1 &
    done
    
    # Wait for all background processes to complete
    echo "Waiting for all requests to complete..."
    wait
    
    SUCCESSFUL=0
    FAILED=0
    
    # Count results
    for i in $(seq 1 $NUM_REQUESTS); do
        result=$(cat /tmp/request_${i}.result 2>/dev/null || echo "FAIL")
        
        if [ "$result" = "OK" ]; then
            ((SUCCESSFUL++))
        else
            ((FAILED++))
        fi
        
        rm -f /tmp/request_${i}.result
    done
    
    END_TIME=$(date +%s.%N)
    DURATION=$(echo "$END_TIME - $START_TIME" | bc 2>/dev/null || echo "N/A")
    
    echo -e "${GREEN}✓${NC} Completed in ${DURATION} seconds"
    echo "  - Successful: ${SUCCESSFUL}"
    echo "  - Failed: ${FAILED}"
    echo ""
    
    # Step 3: Wait for server to process
    echo "Step 3: Waiting for server to process requests..."
    sleep 2
    echo ""
    
    # Step 4: Get final view count
    echo "Step 4: Fetching final view count..."
    FINAL_COUNT=$(get_view_count)
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to fetch final view count. Exiting.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓${NC} Final view count: ${FINAL_COUNT}"
    echo ""
    
    # Step 5: Verify results
    echo "============================================================"
    echo "RESULTS"
    echo "============================================================"
    echo "Initial count:     ${INITIAL_COUNT}"
    echo "Final count:       ${FINAL_COUNT}"
    echo "Expected increase: ${NUM_REQUESTS}"
    
    ACTUAL_INCREASE=$((FINAL_COUNT - INITIAL_COUNT))
    echo "Actual increase:   ${ACTUAL_INCREASE}"
    echo "============================================================"
    echo ""
    
    # Determine success/failure
    EXPECTED_FINAL=$((INITIAL_COUNT + NUM_REQUESTS))
    
    echo "Step 5: Verifying results..."
    echo ""
    
    # Verify by fetching the view count one more time to be absolutely sure
    echo "Fetching view count again to confirm..."
    VERIFICATION_COUNT=$(get_view_count)
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to verify final view count.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓${NC} Verification count: ${VERIFICATION_COUNT}"
    echo ""
    
    if [ "$VERIFICATION_COUNT" -ne "$FINAL_COUNT" ]; then
        echo -e "${YELLOW}⚠ Warning: View count changed between checks (${FINAL_COUNT} → ${VERIFICATION_COUNT})${NC}"
        echo "   This might indicate other concurrent activity on the server."
        FINAL_COUNT=$VERIFICATION_COUNT
        ACTUAL_INCREASE=$((FINAL_COUNT - INITIAL_COUNT))
        echo "   Using verification count for final results."
        echo ""
    fi
    
    echo "============================================================"
    echo "FINAL VERIFICATION"
    echo "============================================================"
    echo "Initial count:     ${INITIAL_COUNT}"
    echo "Final count:       ${FINAL_COUNT}"
    echo "Expected increase: ${NUM_REQUESTS}"
    echo "Actual increase:   ${ACTUAL_INCREASE}"
    echo "============================================================"
    echo ""
    
    if [ "$FINAL_COUNT" -eq "$EXPECTED_FINAL" ]; then
        echo -e "${GREEN}✅ TEST PASSED: View count increased by exactly ${NUM_REQUESTS}!${NC}"
        echo -e "${GREEN}   The system handles concurrency correctly.${NC}"
        exit 0
    else
        DIFFERENCE=$((EXPECTED_FINAL - FINAL_COUNT))
        echo -e "${RED}❌ TEST FAILED: View count mismatch!${NC}"
        echo "   Expected final count: ${EXPECTED_FINAL}"
        echo "   Actual final count:   ${FINAL_COUNT}"
        echo "   Difference:           ${DIFFERENCE} views"
        echo ""
        
        if [ "$DIFFERENCE" -gt 0 ]; then
            echo -e "   ${YELLOW}→ ${DIFFERENCE} views were LOST (race condition detected)${NC}"
            echo "   This indicates the database updates are not properly synchronized."
            echo "   Suggested fixes:"
            echo "     - Use database transactions"
            echo "     - Implement row-level locking"
            echo "     - Use atomic increment operations (e.g., UPDATE SET count = count + 1)"
        else
            ABS_DIFF=$((DIFFERENCE * -1))
            echo -e "   ${YELLOW}→ ${ABS_DIFF} EXTRA views were counted${NC}"
            echo "   This is unusual and might indicate duplicate processing."
        fi
        
        exit 1
    fi
}

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is not installed. Please install curl first.${NC}"
    exit 1
fi

# Check if bc is installed (for floating point arithmetic)
if ! command -v bc &> /dev/null; then
    echo -e "${YELLOW}Warning: bc is not installed. Duration will not be calculated.${NC}"
fi

# Run the test
run_test
