package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface ApplicationDao {
    List<TaskExecutionLog> findByApplicationId(String applicationId);
}
