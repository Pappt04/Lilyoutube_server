package com.group17.lilyoutube_server.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoTranscodingService {

    private final StringRedisTemplate redisTemplate;

    public static final String QUEUE_KEY = "video_transcoding_queue";
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < 2; i++) {
            int workerId = i + 1;
            executorService.submit(() -> runWorker(workerId));
        }
        log.info("Started 2 transcoding workers");
    }

    @PreDestroy
    public void stopWorkers() {
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Exector service did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runWorker(int workerId) {
        log.info("Transcoding worker {} started", workerId);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String filePath = redisTemplate.opsForList().rightPop(QUEUE_KEY, 10, TimeUnit.SECONDS);
                if (filePath != null) {
                    log.info("Worker {} picked up task: {}", workerId, filePath);
                    try {
                        transcodeInPlace(filePath);
                        log.info("Worker {} finished task: {}", workerId, filePath);
                    } catch (Exception e) {
                        log.error("Worker {} failed to transcode file: {}", workerId, filePath, e);
                    }
                }
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.error("Worker {} encountered an error polling the queue", workerId, e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("Transcoding worker {} shutting down", workerId);
    }

    public boolean transcodeVideo(String inputPath, String outputPath) {
        log.info("Starting transcoding: {} -> {}", inputPath, outputPath);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(inputPath);

        // Video Codec: H.264
        command.add("-c:v");
        command.add("libx264");

        command.add("-preset");
        command.add("medium");

        // CRF: 23
        command.add("-crf");
        command.add("23");

        // Audio Codec: AAC
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");

        // Faststart: moves moov atom to the front for web streaming
        command.add("-movflags");
        command.add("+faststart");

        // Scale: Max 1080p width, keep aspect ratio
        command.add("-vf");
        command.add("scale='min(1920,iw)':-2");

        command.add(outputPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Transcoding finished successfully: {}", outputPath);
                return true;
            } else {
                log.error("FFmpeg failed with exit code: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error during transcoding", e);
            return false;
        }
    }

    public boolean transcodeInPlace(String filePath) {
        log.info("Starting in-place transcoding: {}", filePath);
        File originalFile = new File(filePath);
        if (!originalFile.exists()) {
            log.error("File not found for transcoding: {}", filePath);
            return false;
        }

        String tempPath = filePath + ".original";
        File tempFile = new File(tempPath);

        if (!originalFile.renameTo(tempFile)) {
            log.error("Could not rename file for transcoding: {}", filePath);
            return false;
        }

        boolean success = transcodeVideo(tempPath, filePath);

        if (success) {
            if (!tempFile.delete()) {
                log.warn("Could not delete temporary original file: {}", tempPath);
            }
            log.info("In-place transcoding successful: {}", filePath);
        } else {
            if (tempFile.renameTo(originalFile)) {
                log.info("Transcoding failed, original file restored: {}", filePath);
            } else {
                log.error("Transcoding failed AND failed to restore original file: {}", filePath);
            }
        }

        return success;
    }

    /**
     * Enqueues the file for transcoding.
     */
    public void transcodeInPlaceAsync(String filePath) {
        if (filePath == null) {
            log.warn("Attempted to enqueue null file path");
            return;
        }
        log.info("Enqueuing transcoding task: {}", filePath);
        redisTemplate.opsForList().leftPush(QUEUE_KEY, filePath);
    }
}
