package com.group17.lilyoutube_server.monitoring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserActivityMonitor {

    private final Cache<String, Long> activeUsersCache;

    public UserActivityMonitor(MeterRegistry meterRegistry) {

        this.activeUsersCache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();


        Gauge.builder("app.users.active.24h", activeUsersCache, Cache::estimatedSize)
                .description("Number of distinct users active in the last 24 hours")
                .register(meterRegistry);
    }

    public void recordActivity(String userId) {
        if (userId != null) {
            activeUsersCache.put(userId, System.currentTimeMillis());
        }
    }
}
