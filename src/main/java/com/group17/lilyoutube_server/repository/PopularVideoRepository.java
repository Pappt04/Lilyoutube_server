package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {
    List<PopularVideo> findTop3ByRunTimeOrderByScoreDesc(LocalDateTime runTime);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM PopularVideo p WHERE p.runTime = (SELECT MAX(p2.runTime) FROM PopularVideo p2) ORDER BY p.score DESC")
    List<PopularVideo> findTop3ByLatestRun();
}
