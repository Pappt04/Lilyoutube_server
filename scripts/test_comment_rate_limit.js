/**
 * Node.js script to test comment rate limiting (60 comments per hour)
 * Usage: node test_comment_rate_limit.js [postId] [userId] [baseUrl]
 */

const http = require('http');

const POST_ID = process.argv[2] || 1;
const USER_ID = process.argv[3] || 1;
const BASE_URL = process.argv[4] || 'http://localhost:8080';

console.log('Testing comment rate limiting...');
console.log(`Post ID: ${POST_ID}`);
console.log(`User ID: ${USER_ID}`);
console.log(`Base URL: ${BASE_URL}`);
console.log('');

let successCount = 0;
let failCount = 0;
let rateLimitCount = 0;

function sendComment(index) {
    return new Promise((resolve) => {
        const postData = JSON.stringify({
            post_id: parseInt(POST_ID),
            user_id: parseInt(USER_ID),
            text: `Test comment ${index}`
        });

        const options = {
            hostname: BASE_URL.replace('http://', '').replace('https://', '').split(':')[0],
            port: BASE_URL.includes(':') ? BASE_URL.split(':')[2] : 8080,
            path: '/api/comments',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData)
            }
        };

        const req = http.request(options, (res) => {
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                const statusCode = res.statusCode;
                if (statusCode === 200) {
                    console.log(`Comment ${index}: SUCCESS`);
                    successCount++;
                } else if (statusCode === 429) {
                    console.log(`Comment ${index}: RATE LIMIT (429)`);
                    rateLimitCount++;
                    failCount++;
                } else if (statusCode === 401) {
                    console.log(`Comment ${index}: UNAUTHORIZED (401)`);
                    failCount++;
                } else {
                    console.log(`Comment ${index}: FAILED (HTTP ${statusCode})`);
                    console.log(`Response: ${data}`);
                    failCount++;
                }
                resolve();
            });
        });

        req.on('error', (error) => {
            console.error(`Comment ${index}: ERROR - ${error.message}`);
            failCount++;
            resolve();
        });

        req.write(postData);
        req.end();
    });
}

async function runTest() {
    // Try to send 70 comments (10 more than the limit)
    const promises = [];
    for (let i = 1; i <= 70; i++) {
        promises.push(sendComment(i));
        // Small delay to avoid overwhelming the server
        await new Promise(resolve => setTimeout(resolve, 100));
    }

    await Promise.all(promises);

    console.log('');
    console.log('=========================================');
    console.log('Test Results:');
    console.log(`Successful comments: ${successCount}`);
    console.log(`Rate limited (429): ${rateLimitCount}`);
    console.log(`Failed comments: ${failCount}`);
    console.log('=========================================');
    console.log('');
    console.log('Expected: First 60 comments should succeed, remaining should be rate limited (429)');
}

runTest().catch(console.error);

