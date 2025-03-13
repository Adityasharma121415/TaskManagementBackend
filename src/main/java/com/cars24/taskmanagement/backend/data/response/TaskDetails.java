package com.cars24.taskmanagement.backend.data.response;

import lombok.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Data

public class TaskDetails {
    private String funnel;
    private String actorId;
    private String status;
    private Date updatedAt;
    private String taskId;
    private String targetTaskId;
    private int sendbacks;
    private int duration;
    private Map<String, Object> metadata;

}