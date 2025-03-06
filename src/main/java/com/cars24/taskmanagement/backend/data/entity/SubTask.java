//package com.cars24.taskmanagement.backend.data.entity;
//
//
//import lombok.Data;
//import java.time.Instant;
//
//@Data
//public class SubTask {
//    private String taskId;
//    private Instant new_time;        // Set at creation (from createdAt)
//    private Instant todo_time;       // Updated when task moves to TODO
//    private Instant completed_time;  // Updated when task moves to COMPLETED
//    private Instant sendback_time;   // Updated when task is sent back
//    private long duration;           // Cumulative duration (in milliseconds)
//    private int visited;             // Default is 1; increments when todo_time updates, decremented if FAILED
//}
package com.cars24.taskmanagement.backend.data.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class SubTask {
    private String taskId;
    private String status;
    private Instant timestamp;
}
