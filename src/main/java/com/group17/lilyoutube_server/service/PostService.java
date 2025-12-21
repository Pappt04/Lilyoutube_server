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

    public List<PostDTO> getAllVideos() {
        return postRepository.findAll().stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    public PostDTO getVideoById(Long id) {
        return  postMapper.toDto(postRepository.findById(id).orElse(null));
    }

    public PostDTO createVideo(PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);

        Optional<User> current= userRepository.findById(postDTO.getUser_id());

        current.ifPresent(post::setUser);
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    public PostDTO updateVideo(Long id, PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);
        post.setId(id);
        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    public void deleteVideo(Long id) {
        postRepository.deleteById(id);
    }
}
