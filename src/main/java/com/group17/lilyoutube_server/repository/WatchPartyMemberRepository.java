package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.WatchParty;
import com.group17.lilyoutube_server.model.WatchPartyMember;
import com.group17.lilyoutube_server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchPartyMemberRepository extends JpaRepository<WatchPartyMember, Long> {
    List<WatchPartyMember> findByWatchPartyAndActiveTrue(WatchParty watchParty);

    Optional<WatchPartyMember> findByWatchPartyAndUserAndActiveTrue(WatchParty watchParty, User user);

    boolean existsByWatchPartyAndUserAndActiveTrue(WatchParty watchParty, User user);

    long countByWatchPartyAndActiveTrue(WatchParty watchParty);
}
