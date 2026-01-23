package com.group17.lilyoutube_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewReplicaDTO {
    private String videoName;
    private String replicaId;
    private Long views;
}
