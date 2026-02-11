package com.group17.lilyoutube_server.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "posts")
@Data
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String thumbnailPath;

    @Column(nullable = false)
    private String videoPath;

    @Column
    private String location;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime scheduledStartTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long likesCount = 0;

    @Column(nullable = false)
    private long commentsCount = 0;

    @Column(nullable = false)
    private long viewsCount = 0;
}
