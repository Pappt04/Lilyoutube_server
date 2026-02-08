package com.group17.lilyoutube_server.dto.watchparty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinWatchPartyRequest {
    @NotBlank(message = "Room code is required")
    private String roomCode;
}
