package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Data
@Document(collection = "task_execution")
public class TaskExecutionEntity {
    @Id
    private String id;
    private String actorId;
    private String actorType;
    private String applicantId;
    private String applicantType;
    private String applicationId;
    private Boolean automationSupported;
    private String channel;
    private Date createdAt;
    private String entityId;
    private String entityIdentifier;
    private String entityType;
    private String executionType;
    private String funnel; // e.g., "SOURCING", "CREDIT", "CONVERSION", "FULFILMENT"
    private String handledBy;
    private Map<String, Object> inputResourceValueMap;
    private Boolean isCoapplicantTask;
    private Map<String, Object> metadata;
    private Boolean optional;
    private Integer order;
    private String productType;
    private String requestId;
    private Map<String, Object> sendbackMetadata;
    private Date skippedAt;
    private String skippedReason;
    private String status;
    private String statusReason;
    private String taskId;
    private String taskInput;
    private Map<String, Object> taskRuleContext;
    private String templateId;
    private String templateVersion;
    private Date updatedAt;
    private String version;
}
