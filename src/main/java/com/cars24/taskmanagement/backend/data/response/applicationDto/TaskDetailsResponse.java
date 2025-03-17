package com.cars24.taskmanagement.backend.data.response.applicationDto;

import lombok.*;

import java.util.Date;
import java.util.Map;


@Data
@AllArgsConstructor
public class TaskDetailsResponse {
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