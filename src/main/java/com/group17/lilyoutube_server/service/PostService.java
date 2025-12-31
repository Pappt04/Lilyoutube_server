package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.repository.UserRepository;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.util.mappers.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private final PostMapper postMapper;

    public List<PostDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    public PostDTO getPostById(Long id) {
        return postMapper.toDto(postRepository.findById(id).orElse(null));
    }

    public PostDTO getPostByVideoName(String videoName) {
        return postMapper.toDto(postRepository.findByVideoPath(videoName).orElse(null));
    }

    public PostDTO createPost(PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);

        Optional<User> current = userRepository.findById(postDTO.getUser_id());

        current.ifPresent(post::setUser);
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    public PostDTO updatePost(Long id, PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);
        post.setId(id);
        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public void incrementViews(Long id) {
        postRepository.incrementViewsCount(id);
    }
}
