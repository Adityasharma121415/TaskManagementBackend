package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

import java.util.Date;
import java.util.Map;

@Data
@Document(collection = "task_execution")
public class TaskExecutionEntity {
    @Id
    private String id;
    private String entityId;
    private String taskId;
    private String version;
    private int order;
    private String templateId;
    private String templateVersion;
    private String funnel;
    private String channel;
    private String productType;
    private String applicationId;
    private String actorType;
    private String actorId;
    private String status;
    private String statusReason;
    private String executionType;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
