package com.group17.lilyoutube_server.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_party_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"watch_party_id", "user_id"}))
@Data
public class WatchPartyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watch_party_id", nullable = false)
    private WatchParty watchParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false, name = "is_active")
    private boolean active = true;
}
