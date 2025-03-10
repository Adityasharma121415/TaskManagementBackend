package com.cars24.taskmanagement.backend.data.response.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
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
}
