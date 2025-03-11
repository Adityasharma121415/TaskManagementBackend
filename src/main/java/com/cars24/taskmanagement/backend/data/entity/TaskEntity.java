package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;

@Data
public class TaskEntity {

    private String taskId;
    private double duration;
    private int visited;
    private String status;
}
