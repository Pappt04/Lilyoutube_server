package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.model.PopularVideo;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.PostView;
import com.group17.lilyoutube_server.repository.PopularVideoRepository;
import com.group17.lilyoutube_server.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtlService {

    private final PostViewRepository postViewRepository;
    private final PopularVideoRepository popularVideoRepository;

    // Run every day at midnight (00:00:00)
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runPipeline() {
        log.info("Starting Popular Videos ETL Pipeline...");
        LocalDateTime now = LocalDateTime.now();
        LocalDate runDate = now.toLocalDate();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // Extract
        List<PostView> views = postViewRepository.findAllByCreatedAtAfter(sevenDaysAgo);
        log.info("Extracted {} views from the last 7 days.", views.size());

        // Transform
        // Group by Post
        Map<Post, Double> postScores = views.stream()
                .collect(Collectors.groupingBy(
                        PostView::getPost,
                        Collectors.summingDouble(view -> calculateWeight(view.getCreatedAt().toLocalDate(), runDate))
                ));

        List<PopularVideo> popularVideos = postScores.entrySet().stream()
                .sorted(Map.Entry.<Post, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    PopularVideo pv = new PopularVideo();
                    pv.setPost(entry.getKey());
                    pv.setScore(entry.getValue());
                    pv.setRunTime(now);
                    return pv;
                })
                .collect(Collectors.toList());

        // Load
        popularVideoRepository.saveAll(popularVideos);
        log.info("Loaded {} popular videos into the database.", popularVideos.size());
    }

    private double calculateWeight(LocalDate viewDate, LocalDate runDate) {
        long daysAgo = ChronoUnit.DAYS.between(viewDate, runDate);  
        double weight = 7 - daysAgo + 1;
        return Math.max(0, weight); 
    }
}
