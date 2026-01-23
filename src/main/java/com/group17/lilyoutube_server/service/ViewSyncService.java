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

    public ViewSyncDTO getLocalState() {
        Map<Long, Long> localViews = new HashMap<>();
        Set<String> keys = redisTemplate.keys("video_views:*");

        if (keys != null) {
            for (String key : keys) {
                Object val = redisTemplate.opsForHash().get(key, replicaName);
                if (val != null) {
                    Long videoId = Long.parseLong(key.split(":")[1]);
                    localViews.put(videoId, Long.parseLong(val.toString()));
                }
            }
        }
        return new ViewSyncDTO(localViews, replicaName);
    }


    public void receiveSync(ViewSyncDTO syncData) {
        String otherReplica = syncData.getReplicaName();
        Map<Long, Long> views = syncData.getVideoViews();

        if (views != null && otherReplica != null) {
            for (Map.Entry<Long, Long> entry : views.entrySet()) {
                Long vId = entry.getKey();
                Long vCount = entry.getValue();
                if (vId != null && vCount != null) {
                    String key = "video_views:" + vId;
                    String val = vCount.toString();
                    redisTemplate.opsForHash().put(key, otherReplica, val);
                }
            }
            log.info("Received and updated view counts from replica: {}", otherReplica);
        }
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
