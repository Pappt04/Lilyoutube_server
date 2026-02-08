package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.watchparty.CreateWatchPartyRequest;
import com.group17.lilyoutube_server.dto.watchparty.JoinWatchPartyRequest;
import com.group17.lilyoutube_server.dto.watchparty.WatchPartyResponse;
import com.group17.lilyoutube_server.model.User;
import com.group17.lilyoutube_server.service.UserService;
import com.group17.lilyoutube_server.service.WatchPartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/watchparty")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<WatchPartyResponse> createWatchParty(
            @RequestBody CreateWatchPartyRequest request,
            Principal principal) {
        User user = userService.getUserEntityByEmail(principal.getName());
        WatchPartyResponse response = watchPartyService.createWatchParty(user, request.isPublicRoom());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<WatchPartyResponse> joinWatchParty(
            @Valid @RequestBody JoinWatchPartyRequest request,
            Principal principal) {
        User user = userService.getUserEntityByEmail(principal.getName());
        WatchPartyResponse response = watchPartyService.joinWatchParty(request.getRoomCode(), user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leaveWatchParty(
            @PathVariable String roomCode,
            Principal principal) {
        User user = userService.getUserEntityByEmail(principal.getName());
        watchPartyService.leaveWatchParty(roomCode, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomCode}/video/{videoId}")
    public ResponseEntity<WatchPartyResponse> updateCurrentVideo(
            @PathVariable String roomCode,
            @PathVariable Long videoId,
            Principal principal) {
        User user = userService.getUserEntityByEmail(principal.getName());
        WatchPartyResponse response = watchPartyService.updateCurrentVideo(roomCode, user, videoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<WatchPartyResponse> getWatchParty(@PathVariable String roomCode) {
        WatchPartyResponse response = watchPartyService.getWatchParty(roomCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<List<WatchPartyResponse>> getPublicWatchParties() {
        List<WatchPartyResponse> parties = watchPartyService.getPublicWatchParties();
        return ResponseEntity.ok(parties);
    }
}
