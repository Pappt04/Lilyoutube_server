package com.group17.lilyoutube_server.dto.watchparty;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WatchPartyResponse {
    private Long id;
    private String roomCode;
    private Long creatorId;
    private String creatorUsername;
    private Long currentVideoId;
    private String currentVideoTitle;
    private boolean publicRoom;
    private boolean active;
    private LocalDateTime createdAt;
    private List<WatchPartyMemberDTO> members;
    private int memberCount;
}
