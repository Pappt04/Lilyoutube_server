package com.group17.lilyoutube_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularVideoDTO {
    private Long id;
    private LocalDateTime runTime;
    private LocalDateTime pipelineExecutionTime;
    private PostDTO post;
    private Double score;
    private Double popularityScore;
    private Integer rank;
}
