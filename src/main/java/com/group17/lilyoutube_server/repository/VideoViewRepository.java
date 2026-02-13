package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Query(value = "SELECT post_id as postId, COUNT(*) as viewCount, " +
           "EXTRACT(DAY FROM CURRENT_TIMESTAMP - created_at) as daysAgo " +
           "FROM video_views " +
           "WHERE created_at >= :startDate " +
           "GROUP BY post_id, EXTRACT(DAY FROM CURRENT_TIMESTAMP - created_at)",
           nativeQuery = true)
    List<Object[]> findViewCountsGroupedByPostAndDay(@Param("startDate") LocalDateTime startDate);

    long countByPostIdAndCreatedAtAfter(Long postId, LocalDateTime after);
}
