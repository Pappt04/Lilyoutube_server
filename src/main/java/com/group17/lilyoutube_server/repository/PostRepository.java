package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.Post;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByVideoPath(String videoPath);

    List<Post> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :id")
    void incrementViewsCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewsCount = :viewsCount WHERE p.id = :id")
    void updateViewsCount(@Param("id") Long id, @Param("viewsCount") long viewsCount);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :id")
    void incrementLikesCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.likesCount = p.likesCount - 1 WHERE p.id = :id AND p.likesCount > 0")
    void decrementLikesCount(@Param("id") Long id);
}
