package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import java.time.Instant;

@Data
public class SubTask {
    private String taskId;
    private Instant newTime;        // Set at creation (from createdAt)
    private Instant todoTime;       // Updated when task moves to TODO
    private Instant completedTime;  // Updated when task moves to COMPLETED
    private Instant sendbackTime;   // Updated when task is sent back
    private long duration;          // Cumulative duration (in milliseconds)
    private int visited;            // Default is 1; increments when todoTime updates, decremented if FAILED
}
