package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.config.ServerConstants;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Service
@Slf4j
public class ThumbnailCompressionService {

    @Scheduled(cron = "0 0 0 * * *")
    public void compressOldThumbnails() {
        log.info("Starting scheduled thumbnail compression task...");

        Path sourceDir = Paths.get(ServerConstants.thumbDir);
        Path targetDir = Paths.get(ServerConstants.compressedThumbDir);

        if (!Files.exists(sourceDir)) {
            log.warn("Thumbnail directory does not exist: {}", sourceDir);
            return;
        }

        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                log.info("Created compressed thumbnails directory: {}", targetDir);
            }

            Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);

            try (Stream<Path> paths = Files.list(sourceDir)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> isOlderThan(path, oneMonthAgo))
                        .forEach(this::compressIfMissing);
            }

        } catch (IOException e) {
            log.error("Error during thumbnail compression task", e);
        }

        log.info("Finished scheduled thumbnail compression task.");
    }

    private boolean isOlderThan(Path path, Instant threshold) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            return lastModifiedTime.toInstant().isBefore(threshold);
        } catch (IOException e) {
            log.error("Could not get last modified time for file: {}", path, e);
            return false;
        }
    }

    private void compressIfMissing(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        Path targetPath = Paths.get(ServerConstants.compressedThumbDir).resolve(fileName);

        if (Files.exists(targetPath)) {
            log.debug("Skipping compression, already exists: {}", fileName);
            return;
        }

        try {
            log.info("Compressing thumbnail: {}", fileName);
            Thumbnails.of(sourcePath.toFile())
                    .scale(1.0)
                    .outputQuality(0.7)
                    .toFile(targetPath.toFile());
            log.info("Successfully compressed: {}", fileName);
        } catch (IOException e) {
            log.error("Failed to compress thumbnail: {}", fileName, e);
        }
    }
}
