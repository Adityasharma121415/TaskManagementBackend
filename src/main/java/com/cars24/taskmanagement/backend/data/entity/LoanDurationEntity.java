package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "task_execution_time")
public class LoanDurationEntity {

    @Id
    private String id;
    private String applicationId;
    private String entityId;
    private String channel;
    private List<Task> sourcing;
    private List<Task> credit;
    private List<Task> conversion;
    private List<Task> fulfillment;



    @Data
    public static class Task { //
        private String taskId;
        private String new_time;
        private String updatedAt;
        private int sendbacks;
        private long duration;
        private int revisit;      //replaced by visited
    }
}