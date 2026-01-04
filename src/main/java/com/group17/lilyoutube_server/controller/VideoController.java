package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.config.ServerConstants;
import com.group17.lilyoutube_server.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class VideoController {

    private final ThumbnailService thumbnailService;

    @Value("${app.upload.timeout-ms:60000}")
    private long uploadTimeoutMs;

    @PostMapping("/upload-video")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, ServerConstants.videoDir);
    }

    @PostMapping("/upload-picture")
    public ResponseEntity<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> response = handleUpload(file, ServerConstants.thumbDir);
        if (response.getStatusCode().is2xxSuccessful()) {
            thumbnailService.evictThumbnail(response.getBody());
        }
        return response;
    }

    @GetMapping("/videos/{name}")
    public ResponseEntity<Resource> getVideo(@PathVariable String name) {
        return getFileAsResource(ServerConstants.videoDir, name);
    }

    @GetMapping("/thumbnails/{name}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String name) {
        ThumbnailService.CachedThumbnail thumbnail = thumbnailService.getThumbnail(name);
        if (thumbnail != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(thumbnail.contentType()))
                    .body(thumbnail.content());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Resource> getFileAsResource(String directory, String fileName) {
        try {
            Path filePath = Paths.get(directory).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<String> handleUpload(MultipartFile file, String targetDir) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        UUID uuid = UUID.randomUUID();
        String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String newFileName = uuid + (extension.isEmpty() ? "" : "." + extension);
        File targetFile = new File(targetDir + "/" + newFileName);

        try (InputStream inputStream = file.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (System.currentTimeMillis() - startTime > uploadTimeoutMs) {
                    throw new RuntimeException("Upload timed out");
                }
                outputStream.write(buffer, 0, bytesRead);
            }

            return ResponseEntity.ok(newFileName);

        } catch (Exception e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    private String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1);
    }
}