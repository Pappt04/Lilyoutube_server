package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.model.VideoView;
import com.group17.lilyoutube_server.repository.UserRepository;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.LikeRepository;
import com.group17.lilyoutube_server.repository.VideoViewRepository;
import com.group17.lilyoutube_server.util.mappers.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.group17.lilyoutube_server.config.ServerConstants;
import com.group17.lilyoutube_server.config.RabbitConfig;
import com.group17.lilyoutube_server.dto.UploadEventDTO;
import com.group17.lilyoutube_server.proto.UploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private final VideoViewRepository videoViewRepository;
    private final FileService fileService;
    private final VideoTranscodingService transcodingService;

    private final PostMapper postMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.replica-name}")
    private String replicaName;

    public List<PostDTO> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> {
                    PostDTO dto = postMapper.toDto(post);
                    dto.setViewsCount(getMergedViewCountFromRedis(post.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public PostDTO getPostById(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null)
            return null;
        PostDTO dto = postMapper.toDto(post);
        dto.setViewsCount(getMergedViewCountFromRedis(id));
        return dto;
    }

    private Long getMergedViewCountFromRedis(Long videoId) {
        String key = "video_views:" + videoId;
        java.util.Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries.values().stream()
                .mapToLong(v -> Long.parseLong(v.toString()))
                .sum();
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

            // Save as .m3u8 in DB so frontend requests HLS
            postDTO.setVideoPath(videoName.replace(".mp4", ".m3u8"));
            // postDTO.setVideoPath(videoName);

            thumbName = fileService.saveFile(thumbFile, ServerConstants.thumbDir);
            postDTO.setThumbnailPath(thumbName);

            transcodingService.transcodeInPlaceAsync(ServerConstants.videoDir + "/" + videoName);

            Post post = postMapper.toEntity(postDTO);
            Optional<User> current = userRepository.findById(postDTO.getUser_id());
            current.ifPresent(post::setUser);
            post.setCreatedAt(LocalDateTime.now());

            Post savedPost = postRepository.save(post);
            PostDTO savedDto = postMapper.toDto(savedPost);

            sendUploadEvents(savedPost);

            return savedDto;

        } catch (Exception e) {
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
        String key = "video_views:" + id;
        redisTemplate.opsForHash().increment(key, replicaName, 1);

        // Log individual view for ETL pipeline
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            VideoView videoView = VideoView.builder()
                    .post(post)
                    .build();
            videoViewRepository.save(videoView);
        }
    }

    public boolean isLikedByUser(Long postId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }
        return likeRepository.existsByUserIdAndPostId(user.getId(), postId);
    }

    private void sendUploadEvents(Post post) {
        try {
            User user = post.getUser();
            UploadEventDTO jsonEvent = UploadEventDTO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .description(post.getDescription())
                    .thumbnailPath(post.getThumbnailPath())
                    .videoPath(post.getVideoPath())
                    .location(post.getLocation())
                    .tags(post.getTags())
                    .createdAt(post.getCreatedAt().toString())
                    .likesCount(0)
                    .commentsCount(0)
                    .viewsCount(0)
                    .userId(user != null ? user.getId() : null)
                    .username(user != null ? user.getUsername() : null)
                    .email(user != null ? user.getEmail() : null)
                    .firstName(user != null ? user.getFirstName() : null)
                    .lastName(user != null ? user.getLastName() : null)
                    .address(user != null ? user.getAddress() : null)
                    .enabled(user != null && user.isEnabled())
                    .activationToken(user != null ? user.getActivationToken() : null)
                    .build();

            // Send JSON
            byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonEvent);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_JSON, jsonBytes);

            // Send Protobuf
            UploadEvent.Builder protoBuilder = UploadEvent.newBuilder()
                    .setId(post.getId())
                    .setTitle(post.getTitle() != null ? post.getTitle() : "")
                    .setDescription(post.getDescription() != null ? post.getDescription() : "")
                    .setThumbnailPath(post.getThumbnailPath() != null ? post.getThumbnailPath() : "")
                    .setVideoPath(post.getVideoPath() != null ? post.getVideoPath() : "")
                    .setLocation(post.getLocation() != null ? post.getLocation() : "")
                    .setCreatedAt(post.getCreatedAt().toString());

            if (post.getTags() != null) {
                protoBuilder.addAllTags(post.getTags());
            }

            protoBuilder.setLikesCount(0)
                    .setCommentsCount(0)
                    .setViewsCount(0);

            if (user != null) {
                protoBuilder.setUserId(user.getId())
                        .setUsername(user.getUsername() != null ? user.getUsername() : "")
                        .setEmail(user.getEmail() != null ? user.getEmail() : "")
                        .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                        .setLastName(user.getLastName() != null ? user.getLastName() : "")
                        .setAddress(user.getAddress() != null ? user.getAddress() : "")
                        .setEnabled(user.isEnabled())
                        .setActivationToken(user.getActivationToken() != null ? user.getActivationToken() : "");
            }

            byte[] protoBytes = protoBuilder.build().toByteArray();
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_PROTO, protoBytes);

        } catch (Exception e) {
            // We don't want to fail the upload if RabbitMQ fails, but we should log it
            e.printStackTrace();
        }
    }
}
