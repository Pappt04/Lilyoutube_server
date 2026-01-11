package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.CommentDTO;
import com.group17.lilyoutube_server.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/posts/{postId}")
    public ResponseEntity<Page<CommentDTO>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(
                commentService.getCommentsByPost(postId, page)
        );
    }

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(
            @RequestBody CommentDTO commentDto
    ) {
        return ResponseEntity.ok(
                commentService.createComment(commentDto)
        );
    }
}
