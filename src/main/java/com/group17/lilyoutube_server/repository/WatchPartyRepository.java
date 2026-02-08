package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.WatchParty;
import com.group17.lilyoutube_server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {
    Optional<WatchParty> findByRoomCode(String roomCode);

    List<WatchParty> findByCreatorAndActiveTrue(User creator);

    List<WatchParty> findByPublicRoomTrueAndActiveTrue();

    boolean existsByRoomCode(String roomCode);
}
