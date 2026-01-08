package com.group17.lilyoutube_server.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private Long user_id;
    private String username;
    private Long post_id;
    private String text;
    private LocalDateTime createdAt;
    private Long likesCount;
}