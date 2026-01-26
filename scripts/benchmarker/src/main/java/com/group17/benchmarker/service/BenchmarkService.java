package com.group17.benchmarker.service;

import com.group17.benchmarker.model.BenchmarkResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BenchmarkService {
    private final List<BenchmarkResult> results = new CopyOnWriteArrayList<>();

    public void addResult(BenchmarkResult result) {
        results.add(result);
    }

    public List<BenchmarkResult> getResults() {
        return new ArrayList<>(results);
    }
}
