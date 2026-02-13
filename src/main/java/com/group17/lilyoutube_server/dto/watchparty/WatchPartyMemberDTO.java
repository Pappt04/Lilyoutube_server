package com.group17.lilyoutube_server.dto.watchparty;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WatchPartyMemberDTO {
    private Long userId;
    private String username;
    private LocalDateTime joinedAt;
}
