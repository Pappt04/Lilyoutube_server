package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT v FROM PostView v JOIN FETCH v.post WHERE v.createdAt > :date")
    List<PostView> findAllByCreatedAtAfter(LocalDateTime date);
}
