package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;


@Data
@Document(collection = "task_execution_logs")
public class ApplicationEntity {

    @Id
    private String id;
    private String parentId;
    private String taskId;
    private String version;
    private int order;
    private String templateId;
    private String templateVersion;
    private String funnel;
    private String channel;
    private String productType;
    private String applicationId;
    private String entityIdentifier;
    private String entityType;
    private String actorType;
    private String actorId;
    private String status;
    private String executionType;
    private Map<String, Object> metadata;
    private Map<String, Object> inputResourceValueMap;
    private Date createdAt;
    private Date updatedAt;
    private String handledBy;
}
