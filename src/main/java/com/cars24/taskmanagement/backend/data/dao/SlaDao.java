package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;

import java.util.List;

public interface SlaDao {
    public List<TaskExecutionTimeEntity> getTasksByChannel(String channel);
}
