package com.group17.lilyoutube_server.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.group17.lilyoutube_server.dto.CommentDTO;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.CommentRepository;
import com.group17.lilyoutube_server.repository.UserRepository;
import com.group17.lilyoutube_server.util.mappers.CommentMapper;

import com.group17.lilyoutube_server.model.Comment;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
        private final CommentRepository commentRepository;
        private final PostRepository postRepository;
        private final UserRepository userRepository;
        private final CommentMapper commentMapper;

        private final Cache<String, Page<CommentDTO>> commentCache = Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .build();

        public Page<CommentDTO> getCommentsByPost(Long postId, int page) {
                String cacheKey = postId + "-" + page;
                return commentCache.get(cacheKey, k -> {
                        log.info("Fetching comments for post {} page {} from database", postId, page);
                        return commentRepository
                                        .findByPostIdOrderByCreatedAtDesc(postId, (Pageable) PageRequest.of(page, 20))
                                        .map(commentMapper::toDto);
                });
        }

        public CommentDTO createComment(CommentDTO dto) {

                User user = userRepository.findById(dto.getUser_id())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "You must be logged in to comment"));

                Post post = postRepository.findById(dto.getPost_id())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Post not found"));

                // ⏱ rate limit – 60 komment / óra
                LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
                long count = commentRepository.countByUserIdAndCreatedAtAfter(
                                user.getId(),
                                oneHourAgo);

                if (count >= 60) {
                        throw new ResponseStatusException(
                                        HttpStatus.TOO_MANY_REQUESTS,
                                        "Maximum 60 comments per hour");
                }

                Comment comment = new Comment();
                comment.setUser(user);
                comment.setPost(post);
                comment.setText(dto.getText());

                Comment saved = commentRepository.save(comment);

                // Evict cache for this post as a new comment was added
                commentCache.asMap().keySet().removeIf(key -> key.startsWith(post.getId() + "-"));
                log.info("Evicted comment cache for post {}", post.getId());

                return commentMapper.toDto(saved);
        }

}
