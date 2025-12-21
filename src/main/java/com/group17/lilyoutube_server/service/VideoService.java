package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.VideoDTO;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.model.Video;
import com.group17.lilyoutube_server.repository.UserRepository;
import com.group17.lilyoutube_server.repository.VideoRepository;
import com.group17.lilyoutube_server.util.mappers.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    private final VideoMapper videoMapper;

    public List<VideoDTO> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(videoMapper::toDto)
                .collect(Collectors.toList());
    }

    public VideoDTO getVideoById(Long id) {
        return  videoMapper.toDto(videoRepository.findById(id).orElse(null));
    }

    public VideoDTO createVideo(VideoDTO videoDTO) {
        Video video = videoMapper.toEntity(videoDTO);

        Optional<User> current= userRepository.findById(videoDTO.getUser_id());

        current.ifPresent(video::setUser);
        video.setCreatedAt(LocalDateTime.now());

        Video savedVideo = videoRepository.save(video);
        return videoMapper.toDto(savedVideo);
    }

    public VideoDTO updateVideo(Long id, VideoDTO videoDTO) {
        Video video = videoMapper.toEntity(videoDTO);
        video.setId(id);
        Video savedVideo = videoRepository.save(video);
        return videoMapper.toDto(savedVideo);
    }

    public void deleteVideo(Long id) {
        videoRepository.deleteById(id);
    }
}
