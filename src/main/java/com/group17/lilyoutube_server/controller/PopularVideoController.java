package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.PostDTO;
import com.group17.lilyoutube_server.model.PopularVideo;
import com.group17.lilyoutube_server.repository.PopularVideoRepository;
import com.group17.lilyoutube_server.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/popular-videos")
@RequiredArgsConstructor
public class PopularVideoController {

    private final PopularVideoRepository popularVideoRepository;
    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostDTO>> getPopularVideos() {
        List<PopularVideo> popularVideos = popularVideoRepository.findTop3ByLatestRun();

        List<PostDTO> posts = popularVideos.stream()
                .map(pv -> postService.getPostById(pv.getPost().getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(posts);
    }
}
