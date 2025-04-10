package com.cars24.taskmanagement.backend.service.changeStreams;

import java.time.Instant;
import lombok.Data;

@Data
public class TaskExecutionPayload {
    private String taskId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String funnel;
    private String applicationId;
    private String entityId;
    private String channel;
}