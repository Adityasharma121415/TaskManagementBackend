package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;

import java.util.List;


public interface ApplicationDao {
    List<TaskExecutionLog> findTasksByApplicationIdSortedByUpdatedAt(String applicationId);
    List<TaskExecutionLog> findByApplicationId(String applicationId);
}