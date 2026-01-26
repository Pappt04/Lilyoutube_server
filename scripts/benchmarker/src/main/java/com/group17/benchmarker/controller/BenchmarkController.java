package com.group17.benchmarker.controller;

import com.group17.benchmarker.model.BenchmarkResult;
import com.group17.benchmarker.service.BenchmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    public List<BenchmarkResult> getAllBenchmarks() {
        return benchmarkService.getResults();
    }
}
