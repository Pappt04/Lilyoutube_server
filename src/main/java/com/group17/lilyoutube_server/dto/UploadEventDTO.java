package com.group17.lilyoutube_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadEventDTO {
    private Long id;
    private String thumbnailPath;
    private String videoPath;
    private String location;
    private String title;
    private String description;
    private Set<String> tags;
    private String createdAt;
    private long likesCount;
    private long commentsCount;
    private long viewsCount;

    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private boolean enabled;
    private String activationToken;
}
