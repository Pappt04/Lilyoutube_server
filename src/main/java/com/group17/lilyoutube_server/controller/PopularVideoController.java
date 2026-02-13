package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.PopularVideoDTO;
import com.group17.lilyoutube_server.model.PopularVideo;
import com.group17.lilyoutube_server.repository.PopularVideoRepository;
import com.group17.lilyoutube_server.service.PopularVideoETLService;
import com.group17.lilyoutube_server.util.mappers.PopularVideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/popular-videos")
@RequiredArgsConstructor
public class PopularVideoController {

    private final PopularVideoRepository popularVideoRepository;
    private final PopularVideoMapper popularVideoMapper;
    private final PopularVideoETLService etlService;

    /**
     * Get top 3 popular videos from the latest ETL run
     * Accessible to all authenticated users
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PopularVideoDTO>> getPopularVideos() {
        List<PopularVideo> popularVideos = popularVideoRepository.findLatestPopularVideos();
        List<PopularVideoDTO> dtos = popularVideos.stream()
                .map(popularVideoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Manually trigger ETL pipeline
     * Accessible to anyone for testing purposes
     */
    @PostMapping("/trigger-etl")
    public ResponseEntity<Map<String, Object>> triggerETL() {
        long startTime = System.currentTimeMillis();

        etlService.triggerManualETL();

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "ETL pipeline triggered successfully");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("duration_ms", duration);

        return ResponseEntity.ok(response);
    }
}
