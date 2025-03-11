package com.cars24.taskmanagement.backend.data.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "duration")
public class LoanDuration {

    @Id
    private String id;
    private String applicationId;
    private String entityId;
    private String channel;
    private List<Task> sourcing;
    private List<Task> credit;
    private List<Task> conversion;
    private List<Task> fulfillment;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Task { // ✅ Made it static
        private String taskId;
        private String new_time;
        private String updatedAt;
        private int sendbacks;
        private long duration;
        private int visited;
    }
}
