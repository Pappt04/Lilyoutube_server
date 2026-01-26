package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.dto.ViewSyncDTO;
import com.group17.lilyoutube_server.service.ViewSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/views")
@RequiredArgsConstructor
public class InternalController {

    private final ViewSyncService viewSyncService;

    @PostMapping("/sync")
    public ResponseEntity<Void> receiveSync(@RequestBody ViewSyncDTO syncData) {
        viewSyncService.receiveSync(syncData);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/state")
    public ResponseEntity<ViewSyncDTO> getLocalState() {
        return ResponseEntity.ok(viewSyncService.getLocalState());
    }
}
