package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "task_execution_time")
public class TaskExecutionTimeEntity {
    @Id
    private String id;
    private String applicationId;
    private String entityId;
    private String channel;
    private Instant recordDate;


    private List<SubTaskEntity> sourcing = new ArrayList<>();
    private List<SubTaskEntity> credit = new ArrayList<>();
    private List<SubTaskEntity> risk = new ArrayList<>();
    private List<SubTaskEntity> conversion = new ArrayList<>();
    private List<SubTaskEntity> rto = new ArrayList<>();
    private List<SubTaskEntity> fulfillment = new ArrayList<>();
    private List<SubTaskEntity> disbursal = new ArrayList<>();
}
