package com.group17.lilyoutube_server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VideoTranscodingService {

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
     * Async version of in-place transcoding.
     */
    @Async
    public void transcodeInPlaceAsync(String filePath) {
        transcodeInPlace(filePath);
    }
}
