package com.group17.benchmarker.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BenchmarkResult {
    private String type;
    private String id;
    private double deserializationTimeUs;
    private int payloadSize;
    private long timestamp;
}
