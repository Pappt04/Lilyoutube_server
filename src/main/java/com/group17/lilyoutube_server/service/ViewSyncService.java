package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.VideoViewReplicaDTO;
import com.group17.lilyoutube_server.dto.ViewSyncDTO;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewSyncService {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final PostRepository postRepository;

    @Value("${app.replica-name}")
    private String replicaName;

    @Value("${app.peer-urls:}")
    private String peerUrls;

    @Scheduled(fixedRateString = "${app.sync.rate-ms:10000}")
    public void syncWithPeers() {
        if (peerUrls == null || peerUrls.isEmpty()) {
            return;
        }

        String[] peers = peerUrls.split(",");
        for (String peer : peers) {
            try {
                // Pull from peer: GET /api/internal/views/state
                String url = peer.trim() + "/api/internal/views/state";
                ViewSyncDTO remoteState = restTemplate.getForObject(url, ViewSyncDTO.class);
                if (remoteState != null) {
                    receiveSync(remoteState);
                    log.info("Successfully pulled views from peer: {}", peer);
                }
            } catch (Exception e) {
                log.error("Failed to pull views from peer: {}. Error: {}", peer, e.getMessage());
            }
        }
    }

    @Scheduled(fixedRateString = "${app.sync.db-rate-ms:30000}")
    public void syncToDatabase() {
        log.info("Starting view count sync to database...");
        Set<String> keys = redisTemplate.keys("video_views:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                Long videoId = Long.parseLong(key.split(":")[1]);
                Map<Object, Object> replicaViews = redisTemplate.opsForHash().entries(key);

                long totalViews = 0;
                for (Object v : replicaViews.values()) {
                    totalViews += Long.parseLong(v.toString());
                }

                if (totalViews > 0) {
                    postRepository.updateViewsCount(videoId, totalViews);
                    log.debug("Synced video {} views to database: {}", videoId, totalViews);
                }
            } catch (Exception e) {
                log.error("Failed to sync views to DB for key: {}. Error: {}", key, e.getMessage());
            }
        }
        log.info("Finished view count sync to database.");
    }

    public ViewSyncDTO getLocalState() {
        Map<Long, Map<String, Long>> state = new HashMap<>();
        Set<String> keys = redisTemplate.keys("video_views:*");

        if (keys != null) {
            for (String key : keys) {
                Long videoId = Long.parseLong(key.split(":")[1]);
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

                Map<String, Long> replicaMap = new HashMap<>();
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    replicaMap.put(entry.getKey().toString(), Long.parseLong(entry.getValue().toString()));
                }
                state.put(videoId, replicaMap);
            }
        }
        return new ViewSyncDTO(state, replicaName);
    }

    public void receiveSync(ViewSyncDTO syncData) {
        Map<Long, Map<String, Long>> remoteState = syncData.getVideoViews();
        if (remoteState == null)
            return;

        for (Map.Entry<Long, Map<String, Long>> videoEntry : remoteState.entrySet()) {
            Long videoId = videoEntry.getKey();
            Map<String, Long> remoteReplicaCounts = videoEntry.getValue();
            String key = "video_views:" + videoId;

            for (Map.Entry<String, Long> replicaEntry : remoteReplicaCounts.entrySet()) {
                String rName = replicaEntry.getKey();
                Long rCount = replicaEntry.getValue();

                // Get local value for this specific replica
                Object localValObj = redisTemplate.opsForHash().get(key, rName);
                long localCount = (localValObj != null) ? Long.parseLong(localValObj.toString()) : 0L;

                if (rCount > localCount) {
                    redisTemplate.opsForHash().put(key, rName, rCount.toString());
                }
            }
        }
        log.info("Received and merged view count state from replica: {}", syncData.getSourceReplicaName());
    }

    public List<VideoViewReplicaDTO> getReplicaViewsTable() {
        List<VideoViewReplicaDTO> table = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("video_views:*");

        if (keys == null || keys.isEmpty()) {
            return table;
        }

        Map<Long, Post> postMap = postRepository.findAll().stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        for (String key : keys) {
            Long videoId = Long.parseLong(key.split(":")[1]);
            Post post = postMap.get(videoId);
            String videoName = (post != null) ? post.getVideoPath() : "Unknown (" + videoId + ")";

            Map<Object, Object> replicaViews = redisTemplate.opsForHash().entries(key);
            for (Map.Entry<Object, Object> entry : replicaViews.entrySet()) {
                Object rId = entry.getKey();
                Object rViews = entry.getValue();
                if (rId != null && rViews != null) {
                    table.add(new VideoViewReplicaDTO(
                            videoName,
                            rId.toString(),
                            Long.parseLong(rViews.toString())));
                }
            }
        }
        return table;
    }
}
