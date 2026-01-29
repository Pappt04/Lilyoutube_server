package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.dto.UserDTO;
import com.group17.lilyoutube_server.service.LikeService;
import com.group17.lilyoutube_server.service.PostService;
import com.group17.lilyoutube_server.service.UserService;
import com.group17.lilyoutube_server.service.ViewSyncService;
import com.group17.lilyoutube_server.dto.VideoViewReplicaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final LikeService likeService;
    private final ViewSyncService viewSyncService;

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllVideos() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/{name}")
    public ResponseEntity<PostDTO> getPostByVideoName(@PathVariable String name) {
        PostDTO post = postService.getPostByVideoName(name + ".mp4");
        if (post == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(post);
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<PostDTO> createPostWithFiles(
            @RequestPart("post") String postJson,
            @RequestPart("video") MultipartFile video,
            @RequestPart("thumbnail") MultipartFile thumbnail, Principal principal) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        PostDTO postDTO = objectMapper.readValue(postJson, PostDTO.class);

        UserDTO usr = userService.getUserByEmail(principal.getName());
        postDTO.setUser_id(usr.getId());

        return ResponseEntity.ok(postService.createPost(postDTO, video, thumbnail));
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO, Principal principal) {
        UserDTO usr = userService.getUserByEmail(principal.getName());
        postDTO.setUser_id(usr.getId());
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

    @PostMapping("/{name}/view")
    public ResponseEntity<Void> incrementViews(@PathVariable String name) {
        name += ".mp4";
        PostDTO p = postService.getPostByVideoName(name);
        if (p == null)
            return ResponseEntity.notFound().build();

        postService.incrementViews(p.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long id, Principal principal) {
        likeService.likePost(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unlike")
    public ResponseEntity<Void> unlikePost(@PathVariable Long id, Principal principal) {
        likeService.unlikePost(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/liked")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(postService.isLikedByUser(id, principal.getName()));
    }

    @GetMapping("/views/replica-table")
    public ResponseEntity<List<VideoViewReplicaDTO>> getReplicaViewsTable() {
        return ResponseEntity.ok(viewSyncService.getReplicaViewsTable());
    }
}
