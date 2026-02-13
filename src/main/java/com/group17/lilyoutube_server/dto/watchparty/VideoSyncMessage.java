package com.group17.lilyoutube_server.dto.watchparty;

import lombok.Data;

@Data
public class VideoSyncMessage {
    private String type; // "VIDEO_CHANGE", "MEMBER_JOINED", "MEMBER_LEFT"
    private String roomCode;
    private Long videoId;
    private String videoPath;
    private String videoTitle;
    private String userId;
    private String username;
    private int memberCount;
}
