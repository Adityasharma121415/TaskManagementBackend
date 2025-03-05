package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "task_execution_time")
public class TaskExecutionTimeEntity {
    @Id
    private String id;
    private String applicationId;
    private String entityId;
    // Arrays for different funnels
    private List<SubTask> sourcing = new ArrayList<>();
    private List<SubTask> credit = new ArrayList<>();
    private List<SubTask> conversion = new ArrayList<>();
    private List<SubTask> fulfillment = new ArrayList<>();
}
