package com.group17.lilyoutube_server.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
public class PostDTO {
    private Long id;
    private Long user_id;
    private String title;
    private String description;
    private String thumbnailPath;
    private String videoPath;
    private String location;
    private Long likesCount;
    private Long commentsCount;
    private Long viewsCount;
    private Collection<String> tags;
    private LocalDateTime createdAt;
}
