package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import java.time.Instant;

@Data
public class SubTaskEntity {
    private String taskId;
    private Instant new_time;
    private Instant todo_time;
    private Instant completed_time;
    private Instant sendback_time;
    private long duration;
    private int visited;

    public SubTaskEntity(String taskId) {
        this.taskId = taskId;
        this.new_time = Instant.now();
        this.visited = 1;
    }

    public void updateStatus(String status) {
        switch (status.toUpperCase()) {
            case "TODO":
                this.todo_time = Instant.now();
                this.visited++;
                break;

            case "COMPLETED":
                this.completed_time = Instant.now();
                if (this.todo_time != null && this.visited > 1) {  // Ensure at least one visit
                    this.duration += this.completed_time.toEpochMilli() - this.todo_time.toEpochMilli();
                }
                break;

            case "SENDBACK":
                this.sendback_time = Instant.now();
                if (this.todo_time != null) {
                    this.duration += this.sendback_time.toEpochMilli() - this.todo_time.toEpochMilli();
                }
                break;

            case "FAILED":
                this.visited = Math.max(0, this.visited - 1);
                break;
        }
    }
}
