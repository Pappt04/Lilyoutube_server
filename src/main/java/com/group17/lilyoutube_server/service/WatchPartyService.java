package com.group17.lilyoutube_server.service;

import com.group17.lilyoutube_server.dto.watchparty.WatchPartyMemberDTO;
import com.group17.lilyoutube_server.dto.watchparty.WatchPartyResponse;
import com.group17.lilyoutube_server.model.Post;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.model.WatchParty;
import com.group17.lilyoutube_server.model.WatchPartyMember;
import com.group17.lilyoutube_server.repository.PostRepository;
import com.group17.lilyoutube_server.repository.WatchPartyMemberRepository;
import com.group17.lilyoutube_server.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchPartyService {
    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyMemberRepository watchPartyMemberRepository;
    private final PostRepository postRepository;

    private static final String ROOM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public WatchPartyResponse createWatchParty(User creator, boolean isPublic) {
        log.info("Creating watch party for user: {}", creator.getUsername());

        WatchParty watchParty = new WatchParty();
        watchParty.setCreator(creator);
        watchParty.setPublicRoom(isPublic);
        watchParty.setRoomCode(generateUniqueRoomCode());
        watchParty.setActive(true);

        watchParty = watchPartyRepository.save(watchParty);

        // Creator automatically joins the room
        WatchPartyMember member = new WatchPartyMember();
        member.setWatchParty(watchParty);
        member.setUser(creator);
        member.setActive(true);
        watchPartyMemberRepository.save(member);

        log.info("Watch party created with room code: {}", watchParty.getRoomCode());
        return toResponse(watchParty);
    }

    @Transactional
    public WatchPartyResponse joinWatchParty(String roomCode, User user) {
        log.info("User {} attempting to join room: {}", user.getUsername(), roomCode);

        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found"));

        if (!watchParty.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Watch party is no longer active");
        }

        // Check if user is already a member
        if (watchPartyMemberRepository.existsByWatchPartyAndUserAndActiveTrue(watchParty, user)) {
            log.info("User {} is already a member of room {}", user.getUsername(), roomCode);
            return toResponse(watchParty);
        }

        WatchPartyMember member = new WatchPartyMember();
        member.setWatchParty(watchParty);
        member.setUser(user);
        member.setActive(true);
        watchPartyMemberRepository.save(member);

        log.info("User {} joined room {}", user.getUsername(), roomCode);
        return toResponse(watchParty);
    }

    @Transactional
    public void leaveWatchParty(String roomCode, User user) {
        log.info("User {} leaving room: {}", user.getUsername(), roomCode);

        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found"));

        WatchPartyMember member = watchPartyMemberRepository
                .findByWatchPartyAndUserAndActiveTrue(watchParty, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a member"));

        member.setActive(false);
        watchPartyMemberRepository.save(member);

        log.info("User {} left room {}", user.getUsername(), roomCode);
    }

    @Transactional
    public WatchPartyResponse updateCurrentVideo(String roomCode, User creator, Long videoId) {
        log.info("Updating current video for room {} to video {}", roomCode, videoId);

        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found"));

        if (!watchParty.getCreator().getId().equals(creator.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can change the video");
        }

        if (!watchParty.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Watch party is no longer active");
        }

        Post video = postRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        watchParty.setCurrentVideo(video);
        watchParty = watchPartyRepository.save(watchParty);

        log.info("Current video updated for room {}", roomCode);
        return toResponse(watchParty);
    }

    public WatchPartyResponse getWatchParty(String roomCode) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Watch party not found"));

        return toResponse(watchParty);
    }

    public List<WatchPartyResponse> getPublicWatchParties() {
        return watchPartyRepository.findByPublicRoomTrueAndActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = generateRoomCode();
        } while (watchPartyRepository.existsByRoomCode(roomCode));
        return roomCode;
    }

    private String generateRoomCode() {
        StringBuilder code = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            code.append(ROOM_CODE_CHARS.charAt(random.nextInt(ROOM_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private WatchPartyResponse toResponse(WatchParty watchParty) {
        WatchPartyResponse response = new WatchPartyResponse();
        response.setId(watchParty.getId());
        response.setRoomCode(watchParty.getRoomCode());
        response.setCreatorId(watchParty.getCreator().getId());
        response.setCreatorUsername(watchParty.getCreator().getUsername());
        response.setPublicRoom(watchParty.isPublicRoom());
        response.setActive(watchParty.isActive());
        response.setCreatedAt(watchParty.getCreatedAt());

        if (watchParty.getCurrentVideo() != null) {
            response.setCurrentVideoId(watchParty.getCurrentVideo().getId());
            response.setCurrentVideoTitle(watchParty.getCurrentVideo().getTitle());
        }

        List<WatchPartyMember> members = watchPartyMemberRepository
                .findByWatchPartyAndActiveTrue(watchParty);

        response.setMemberCount(members.size());
        response.setMembers(members.stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList()));

        return response;
    }

    private WatchPartyMemberDTO toMemberDTO(WatchPartyMember member) {
        WatchPartyMemberDTO dto = new WatchPartyMemberDTO();
        dto.setUserId(member.getUser().getId());
        dto.setUsername(member.getUser().getUsername());
        dto.setJoinedAt(member.getJoinedAt());
        return dto;
    }
}
