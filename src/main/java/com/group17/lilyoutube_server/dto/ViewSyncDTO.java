package com.group17.lilyoutube_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewSyncDTO {
    // videoId -> (replicaName -> counterValue)
    private Map<Long, Map<String, Long>> videoViews;
    private String sourceReplicaName;
}
