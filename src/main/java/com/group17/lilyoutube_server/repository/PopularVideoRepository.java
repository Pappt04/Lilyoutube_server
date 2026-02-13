package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {

    @Query("SELECT pv FROM PopularVideo pv WHERE pv.pipelineExecutionTime = " +
           "(SELECT MAX(p.pipelineExecutionTime) FROM PopularVideo p) " +
           "ORDER BY pv.rank ASC")
    List<PopularVideo> findLatestPopularVideos();

    @Modifying
    @Transactional
    @Query("DELETE FROM PopularVideo pv WHERE pv.pipelineExecutionTime < :before")
    void deleteByPipelineExecutionTimeBefore(@Param("before") LocalDateTime before);
}
