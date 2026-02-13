package com.group17.lilyoutube_server.scheduled;

import com.group17.lilyoutube_server.model.WatchParty;
import com.group17.lilyoutube_server.repository.WatchPartyMemberRepository;
import com.group17.lilyoutube_server.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to clean up inactive or empty watch parties
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchPartyCleanupTask {

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyMemberRepository watchPartyMemberRepository;

    /**
     * Runs every hour to clean up:
     * 1. Watch parties with 0 active members
     * 2. Inactive watch parties older than 24 hours
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void cleanupWatchParties() {
        log.info("Starting watch party cleanup task");

        int deactivatedCount = 0;
        int deletedCount = 0;

        // Find all active watch parties
        List<WatchParty> activeParties = watchPartyRepository.findByActiveTrue();

        for (WatchParty party : activeParties) {
            long activeMemberCount = watchPartyMemberRepository.countByWatchPartyAndActiveTrue(party);

            // Deactivate parties with no active members
            if (activeMemberCount == 0) {
                log.info("Deactivating empty watch party: {}", party.getRoomCode());
                party.setActive(false);
                watchPartyRepository.save(party);
                deactivatedCount++;
            }
        }

        // Delete inactive watch parties older than 24 hours
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<WatchParty> oldInactiveParties = watchPartyRepository
                .findByActiveFalseAndCreatedAtBefore(cutoffTime);

        for (WatchParty party : oldInactiveParties) {
            log.info("Deleting old inactive watch party: {} (created: {})",
                    party.getRoomCode(), party.getCreatedAt());

            // Delete all members first
            watchPartyMemberRepository.deleteByWatchParty(party);

            // Then delete the party
            watchPartyRepository.delete(party);
            deletedCount++;
        }

        log.info("Watch party cleanup completed: {} deactivated, {} deleted",
                deactivatedCount, deletedCount);
    }

    /**
     * Alternative: Cleanup immediately when requested (can be called via endpoint if needed)
     */
    @Transactional
    public void cleanupNow() {
        log.info("Manual watch party cleanup triggered");
        cleanupWatchParties();
    }
}
