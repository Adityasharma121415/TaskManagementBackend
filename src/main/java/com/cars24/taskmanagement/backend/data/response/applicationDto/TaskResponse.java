package com.cars24.taskmanagement.backend.data.response.applicationDto;



import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class TaskResponse {
    private String taskId;
    private int order;
    private String handledBy;
    private Date createdAt;
    private List<StatusLogResponse> statusHistory;
    private String targetTaskId;
    private long duration;
    private int sendbacks;
    private int visited;



}
