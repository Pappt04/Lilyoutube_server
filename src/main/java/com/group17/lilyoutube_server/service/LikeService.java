package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.PostLike;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.repository.LikeRepository;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likePost(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (likeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Post already liked");
        }

        likeRepository.save(new PostLike(user, post));
        postRepository.incrementLikesCount(post.getId());
    }

    @Transactional
    public void unlikePost(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        if (likeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            likeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
            postRepository.decrementLikesCount(post.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not liked by user");
        }
    }

    public boolean isLikedByUser(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null)
            return false;
        return likeRepository.existsByUserIdAndPostId(user.getId(), postId);
    }
}
