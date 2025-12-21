package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllVideos() {
        return ResponseEntity.ok(postService.getAllVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getVideoById(@PathVariable Long id)
    {
        PostDTO video = postService.getVideoById(id);
        if (video == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(video);
    }

    @PostMapping
    public ResponseEntity<PostDTO> createVideo(@RequestBody PostDTO postDTO) {
        return ResponseEntity.ok(postService.createVideo(postDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> updateVideo(@PathVariable Long id, @RequestBody PostDTO postDTO) {
        return ResponseEntity.ok(postService.updateVideo(id, postDTO));
    }

    @DeleteMapping("/{id}")
    public void deleteVideo(@PathVariable Long id) {
        postService.deleteVideo(id);
    }
}
