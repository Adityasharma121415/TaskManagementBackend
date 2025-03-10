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
    private Instant updatedAt;
    private Instant createdAt;
    private int sendbacks;
    private long duration;
    private int visited;


    public SubTaskEntity(String taskId, Instant createdAt) {
        this.taskId = taskId;
//        this.new_time = updatedAt;
        this.new_time = createdAt;
        this.visited = 0;
        this.sendbacks = 0;
    }

    public void updateStatus(String status, Instant updatedAt) {
        switch (status.toUpperCase()) {
            case "TODO":
                this.todo_time = updatedAt;
                this.visited++;
                break;

            case "COMPLETED":
                if (this.todo_time != null && this.visited > 1) {
                    this.completed_time = updatedAt;
                    this.duration += updatedAt.toEpochMilli() - this.todo_time.toEpochMilli();
                    this.sendbacks++;
                }
                else if (this.new_time != null && (this.visited == 1 || this.todo_time == null)) {
                    this.completed_time = updatedAt;
                    this.duration += updatedAt.toEpochMilli() - this.new_time.toEpochMilli();
                    this.sendbacks++;
                }
                // If neither condition is met, completed_time remains null
                break;

            case "SENDBACK":
                this.sendback_time = updatedAt; // Always set this first

                if (this.todo_time != null && this.visited > 1) {
                    this.duration += updatedAt.toEpochMilli() - this.todo_time.toEpochMilli();
                }
                else if (this.new_time != null && this.visited == 1) {
                    this.duration += updatedAt.toEpochMilli() - this.new_time.toEpochMilli();
                }
                break;

            case "FAILED":
                this.visited = Math.max(0, this.visited - 1);
                break;

            case "NEW":
                this.new_time = updatedAt;
                break;
        }

        // Always update the updatedAt field
        this.updatedAt = updatedAt;
    }
}
