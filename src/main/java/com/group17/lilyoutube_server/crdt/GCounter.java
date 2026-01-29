package com.group17.lilyoutube_server.crdt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GCounter {
    private final Map<String, Long> counters;

    public GCounter() {
        this.counters = new HashMap<>();
    }

    public GCounter(Map<String, Long> counters) {
        this.counters = new HashMap<>(counters);
    }

    public void increment(String replicaId, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Counter delta must be positive");
        }
        counters.put(replicaId, counters.getOrDefault(replicaId, 0L) + delta);
    }

    public void merge(GCounter other) {
        for (Map.Entry<String, Long> entry : other.counters.entrySet()) {
            String replicaId = entry.getKey();
            Long otherValue = entry.getValue();
            Long localValue = counters.getOrDefault(replicaId, 0L);
            counters.put(replicaId, Math.max(localValue, otherValue));
        }
    }

    public long getValue() {
        return counters.values().stream().mapToLong(Long::longValue).sum();
    }

    public Map<String, Long> getCounters() {
        return new HashMap<>(counters);
    }
}
