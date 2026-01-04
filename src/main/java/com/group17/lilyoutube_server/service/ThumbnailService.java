package com.group17.lilyoutube_server.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.group17.lilyoutube_server.config.ServerConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ThumbnailService {

    public record CachedThumbnail(byte[] content, String contentType) {
    }

    private final Cache<String, CachedThumbnail> thumbnailCache;

    public ThumbnailService() {
        this.thumbnailCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(500)
                .build();
    }

    public CachedThumbnail getThumbnail(String name) {
        return thumbnailCache.get(name, k -> {
            try {
                Path filePath = Paths.get(ServerConstants.thumbDir).resolve(k).normalize();
                if (!Files.exists(filePath)) {
                    log.error("Thumbnail not found: {}", filePath);
                    return null;
                }
                byte[] content = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "image/jpeg";
                }
                log.info("Loaded thumbnail from disk and cached: {}", k);
                return new CachedThumbnail(content, contentType);
            } catch (IOException e) {
                log.error("Error reading thumbnail from disk: {}", k, e);
                return null;
            }
        });
    }

    public void evictThumbnail(String name) {
        thumbnailCache.invalidate(name);
    }
}
