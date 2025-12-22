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
        return ResponseEntity.ok(postService.getAllPosts());
    }

/*    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id)
    {
        PostDTO post = postService.getPostById(id);
        if (post == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(post);
    }*/

    @GetMapping("/{name}")
    public ResponseEntity<PostDTO> getPostByVideoId(@PathVariable String name)
    {
        PostDTO post = postService.getPostByVideoName(name+".mp4");
        if(post == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO) {
        return ResponseEntity.ok(postService.createPost(postDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long id, @RequestBody PostDTO postDTO) {
        return ResponseEntity.ok(postService.updatePost(id, postDTO));
    }

    @DeleteMapping("/{id}")
    public void deleteVideo(@PathVariable Long id) {
        postService.deletePost(id);
    }
}
