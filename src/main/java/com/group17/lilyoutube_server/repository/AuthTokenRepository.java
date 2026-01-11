package com.group17.lilyoutube_server.repository;

import com.group17.lilyoutube_server.model.AuthToken;
import com.group17.lilyoutube_server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);
    void deleteByUser(User user);
    void deleteByExpiresAtBefore(Instant now);
}
