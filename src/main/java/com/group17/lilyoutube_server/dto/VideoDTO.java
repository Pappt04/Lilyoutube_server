package com.group17.lilyoutube_server.dto;

import lombok.Data;

import java.util.Collection;

@Data
public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private String thumbnailPath;
    private String videoPath;
    private String location;
    private Collection<String> tags;
}
