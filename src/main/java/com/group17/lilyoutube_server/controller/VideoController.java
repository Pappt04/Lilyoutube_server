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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class VideoController {

    private final ThumbnailService thumbnailService;
    private final FileService fileService;
    private final com.group17.lilyoutube_server.service.PostService postService;

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
    public ResponseEntity<?> getVideo(@PathVariable String name) {
        if (name.endsWith(".m3u8")) {
            return getHlsPlaylist(name);
        } else if (name.endsWith(".ts")) {
            return getHlsSegment(name);
        }
        return getFileAsResource(ServerConstants.videoDir, name);
    }

    private ResponseEntity<?> getHlsPlaylist(String playlistName) {
        try {
            // DB stores "uuid.m3u8" now
            com.group17.lilyoutube_server.dto.PostDTO post = postService.getPostByVideoName(playlistName);

            if (post == null) {
                 post = postService.getPostByVideoName(playlistName.replace(".m3u8", ".mp4"));
            }

            if (post != null && post.getScheduledStartTime() != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                if (now.isBefore(post.getScheduledStartTime())) {
                    return ResponseEntity.status(403).body("Video is not yet available.");
                }

                long secondsSinceStart = java.time.temporal.ChronoUnit.SECONDS.between(post.getScheduledStartTime(), now);
                return getFilteredPlaylist(playlistName, secondsSinceStart);
            }

            return getFileAsResource(ServerConstants.videoDir, playlistName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<?> getHlsSegment(String segmentName) {
        try {
            return getFileAsResource(ServerConstants.videoDir, segmentName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<?> getFilteredPlaylist(String playlistName, long secondsSinceStart) throws IOException {
        Path filePath = Paths.get(ServerConstants.videoDir).resolve(playlistName).normalize();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        java.util.List<String> lines = Files.readAllLines(filePath);

        
        List<String> header = new ArrayList<>();
        List<HlsSegment> segments = new ArrayList<>();
        
        boolean headerDone = false;
        double currentDuration = 0.0;
        
        double targetTagDuration = 10.0; // Default fallback

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                try {
                     targetTagDuration = Double.parseDouble(line.substring(22));
                } catch (Exception e) {}
                header.add(line);
            } else if (line.startsWith("#EXT-X-ENDLIST")) {
                continue; 
            } else if (line.startsWith("#EXTINF:")) {
                headerDone = true;
                String durationStr = line.substring(8, line.indexOf(","));
                double duration = Double.parseDouble(durationStr);
                
                String filename = "";
                if (i + 1 < lines.size()) {
                    filename = lines.get(i + 1);
                    i++;
                }
                
                segments.add(new HlsSegment(duration, line, filename));
                
            } else if (!headerDone) {
                header.add(line);
            }
        }
        
        // Calculate Total Duration
        double totalDuration = segments.stream().mapToDouble(s -> s.duration).sum();

        if (secondsSinceStart >= totalDuration) {
             List<String> response = new ArrayList<>(header);
             for(HlsSegment s : segments) {
                 response.add(s.extinf);
                 response.add(s.filename);
             }
             response.add("#EXT-X-ENDLIST");
             return createM3u8Response(response);
        }
        
        int windowSize = 3;

        int liveIndex = 0;
        double accumulated = 0.0;
        for (int i = 0; i < segments.size(); i++) {
            accumulated += segments.get(i).duration;
            if (accumulated >= secondsSinceStart) {
                liveIndex = i;
                break;
            }
        }
        if (accumulated < secondsSinceStart) liveIndex = segments.size() - 1;

        int startIndex = Math.max(0, liveIndex - windowSize + 1);
        int endIndex = liveIndex; // inclusive
        
        List<String> response = new ArrayList<>(header);
        boolean hasSequence = false;
        for(int i=0; i<response.size(); i++) {
            if (response.get(i).startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                response.set(i, "#EXT-X-MEDIA-SEQUENCE:" + startIndex);
                hasSequence = true;
                break;
            }
        }
        if (!hasSequence) {
            response.add(1, "#EXT-X-MEDIA-SEQUENCE:" + startIndex);
        }

        for (int i = startIndex; i <= endIndex; i++) {
            HlsSegment s = segments.get(i);
            response.add(s.extinf);
            response.add(s.filename);
        }
        
        return createM3u8Response(response);
    }
    
    private ResponseEntity<byte[]> createM3u8Response(List<String> lines) {
        String content = String.join("\n", lines);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static class HlsSegment {
        double duration;
        String extinf;
        String filename;
        
        public HlsSegment(double duration, String extinf, String filename) {
            this.duration = duration;
            this.extinf = extinf;
            this.filename = filename;
        }
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