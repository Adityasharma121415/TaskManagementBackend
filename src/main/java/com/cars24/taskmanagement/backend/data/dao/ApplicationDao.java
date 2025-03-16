package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ApplicationDao {
    List<TaskExecutionLogEntity> findByApplicationId(String applicationId);
    List<TaskExecutionLogEntity> findTasksByApplicationIdSortedByUpdatedAt(String applicationId);
    Map<String, Object> findTasksAndLoanDurationByApplicationId(String applicationId);
}