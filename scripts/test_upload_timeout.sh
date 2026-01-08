#!/bin/bash

# Configuration
URL="http://localhost:8080/api/posts"
VIDEO_FILE="/home/papp/Videos/menzans_backgroundlocation.mp4"
THUMB_FILE="/home/papp/Pictures/ftnsebudi.jpg"
POST_DATA='{"user_id":1, "title":"Test Video", "description":"Testing timeout", "tags":["test"]}'
MAX_TIME=5 # seconds to consider "too long" for this test script

# Create dummy files if they don't exist
if [ ! -f "$VIDEO_FILE" ]; then
    head -c 10M /dev/zero > "$VIDEO_FILE"
fi
if [ ! -f "$THUMB_FILE" ]; then
    head -c 1M /dev/zero > "$THUMB_FILE"
fi

echo "Testing upload to $URL ..."

# Start timer
START_TIME=$(date +%s)

# Perform upload
# Using --limit-rate to simulate slow upload if needed, 
# but here we just measure how long it takes.
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL" \
  -H "Content-Type: multipart/form-data" \
  -F "post=$POST_DATA" \
  -F "video=@$VIDEO_FILE" \
  -F "thumbnail=@$THUMB_FILE")

# End timer
END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo "Upload finished with HTTP status: $RESPONSE"
echo "Time elapsed: $ELAPSED seconds"

if [ "$RESPONSE" == "500" ]; then
    echo "Server returned 500. This might be due to the timeout logic triggering (check server logs)."
fi

if [ $ELAPSED -gt $MAX_TIME ]; then
    echo "WARNING: Upload took too long ($ELAPSED seconds > $MAX_TIME seconds)!"
    exit 1
else
    echo "SUCCESS: Upload completed within $MAX_TIME seconds."
    exit 0
fi
