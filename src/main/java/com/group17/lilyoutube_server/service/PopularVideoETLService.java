package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.model.PopularVideo;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.repository.PopularVideoRepository;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularVideoETLService {

    private final VideoViewRepository videoViewRepository;
    private final PopularVideoRepository popularVideoRepository;
    private final PostRepository postRepository;

    /**
     * ETL Pipeline for Popular Videos
     * Runs daily at midnight (cron: 0 0 0 * * *)
     * Can also be triggered manually via API
     */
    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    @Transactional
    public void runETLPipeline() {
        log.info("Starting ETL pipeline for popular videos...");

        LocalDateTime executionTime = LocalDateTime.now();

        try {
            // EXTRACT: Get view data from last 7 days
            LocalDateTime sevenDaysAgo = executionTime.minusDays(7);
            List<Object[]> viewData = videoViewRepository.findViewCountsGroupedByPostAndDay(sevenDaysAgo);

            // TRANSFORM: Calculate weighted popularity scores
            Map<Long, Double> popularityScores = calculatePopularityScores(viewData);

            // Get top 3 videos
            List<Map.Entry<Long, Double>> top3 = popularityScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            // LOAD: Save results to database
            List<PopularVideo> popularVideos = new ArrayList<>();
            int rank = 1;
            for (Map.Entry<Long, Double> entry : top3) {
                Post post = postRepository.findById(entry.getKey()).orElse(null);
                if (post != null) {
                    PopularVideo popularVideo = PopularVideo.builder()
                            .runTime(executionTime)
                            .pipelineExecutionTime(executionTime)
                            .post(post)
                            .score(entry.getValue())
                            .popularityScore(entry.getValue())
                            .rank(rank++)
                            .build();
                    popularVideos.add(popularVideo);
                }
            }

            popularVideoRepository.saveAll(popularVideos);

            // Clean up old ETL results (keep last 30 days)
            LocalDateTime thirtyDaysAgo = executionTime.minusDays(30);
            popularVideoRepository.deleteByPipelineExecutionTimeBefore(thirtyDaysAgo);

            log.info("ETL pipeline completed successfully. Found {} popular videos.", popularVideos.size());

        } catch (Exception e) {
            log.error("ETL pipeline failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate popularity scores based on weighted views
     * Formula: views from x days ago have weight (7-x+1)
     * - Views from today (0 days ago) = weight 8
     * - Views from 1 day ago = weight 7
     * - Views from 7 days ago = weight 1
     */
    private Map<Long, Double> calculatePopularityScores(List<Object[]> viewData) {
        Map<Long, Double> scores = new HashMap<>();

        for (Object[] row : viewData) {
            Long postId = ((Number) row[0]).longValue();
            Long viewCount = ((Number) row[1]).longValue();
            Double daysAgo = ((Number) row[2]).doubleValue();

            // Calculate weight: (7 - daysAgo + 1)
            // But daysAgo can be fractional, so we use floor for day calculation
            int dayInt = (int) Math.floor(daysAgo);
            double weight = Math.max(1.0, 8.0 - dayInt); // 8-dayInt gives: today=8, yesterday=7, 7days ago=1

            double contribution = viewCount * weight;
            scores.merge(postId, contribution, Double::sum);
        }

        return scores;
    }

    /**
     * Manual trigger for ETL pipeline (for testing/admin purposes)
     */
    public void triggerManualETL() {
        log.info("Manual ETL pipeline trigger requested");
        runETLPipeline();
    }
}
