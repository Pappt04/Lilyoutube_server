package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.VideoDTO;
import com.group17.lilyoutube_server.service.UserService;
import com.group17.lilyoutube_server.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<List<VideoDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideoById(@PathVariable Long id)
    {
        VideoDTO video = videoService.getVideoById(id);
        if (video == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(video);
    }

    @PostMapping
    public ResponseEntity<VideoDTO> createVideo(@RequestBody VideoDTO videoDTO) {
        return ResponseEntity.ok(videoService.createVideo(videoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoDTO> updateVideo(@PathVariable Long id,@RequestBody VideoDTO videoDTO) {
        return ResponseEntity.ok(videoService.updateVideo(id,videoDTO));
    }

    @DeleteMapping("/{id}")
    public void deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
    }
}
