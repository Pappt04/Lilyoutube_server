package com.group17.lilyoutube_server.dto.watchparty;

import lombok.Data;

@Data
public class VideoSyncMessage {
    private String type; // "video_change", "member_joined", "member_left"
    private Long videoId;
    private String videoTitle;
    private String username;
    private int memberCount;
}
