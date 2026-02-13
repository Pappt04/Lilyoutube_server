package com.group17.lilyoutube_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopularVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "run_time")
    private LocalDateTime runTime;

    @Column
    private LocalDateTime pipelineExecutionTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column
    private Double score;

    @Column
    private Double popularityScore;

    @Column
    private Integer rank;
}
