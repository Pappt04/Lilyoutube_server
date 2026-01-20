package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.repository.UserRepository;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.LikeRepository;
import com.group17.lilyoutube_server.util.mappers.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.group17.lilyoutube_server.config.ServerConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FileService fileService;
    private final VideoTranscodingService transcodingService;

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

    @Transactional(rollbackFor = Exception.class)
    public PostDTO createPost(PostDTO postDTO, MultipartFile videoFile, MultipartFile thumbFile) {
        String videoName = null;
        String thumbName = null;

        try {
            videoName = fileService.saveFile(videoFile, ServerConstants.videoDir);
            postDTO.setVideoPath(videoName);

            thumbName = fileService.saveFile(thumbFile, ServerConstants.thumbDir);
            postDTO.setThumbnailPath(thumbName);

            transcodingService.transcodeInPlaceAsync(ServerConstants.videoDir + "/" + videoName);

            Post post = postMapper.toEntity(postDTO);
            Optional<User> current = userRepository.findById(postDTO.getUser_id());
            current.ifPresent(post::setUser);
            post.setCreatedAt(LocalDateTime.now());

            Post savedPost = postRepository.save(post);

            return postMapper.toDto(savedPost);

        } catch (Exception e) {
            // Cleanup files if any were uploaded
            if (videoName != null) {
                fileService.deleteFile(ServerConstants.videoDir + "/" + videoName);
            }
            if (thumbName != null) {
                fileService.deleteFile(ServerConstants.thumbDir + "/" + thumbName);
            }
            throw new RuntimeException("Post creation failed: " + e.getMessage(), e);
        }
    }

    // Keeping the original createPost for internal use or simple cases
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

    public boolean isLikedByUser(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }
        return likeRepository.existsByUserIdAndPostId(user.getId(), postId);
    }
}
