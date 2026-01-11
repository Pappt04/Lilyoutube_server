package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.config.ServerConstants;
import com.group17.lilyoutube_server.service.FileService;
import com.group17.lilyoutube_server.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class VideoController {

    private final ThumbnailService thumbnailService;
    private final FileService fileService;

    @PostMapping("/upload-video")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(fileService.saveFile(file, ServerConstants.videoDir));
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-picture")
    public ResponseEntity<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileService.saveFile(file, ServerConstants.thumbDir);
            thumbnailService.evictThumbnail(fileName);
            return ResponseEntity.ok(fileName);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
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
}