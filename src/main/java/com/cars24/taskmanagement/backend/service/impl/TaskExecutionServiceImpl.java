package com.cars24.taskmanagement.backend.service.impl;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionRepository;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl implements TaskExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionServiceImpl.class);

    private final TaskExecutionRepository taskExecutionRepository;

    @Override
    public List<TaskExecutionEntity> findAll() {
        try {
            List<TaskExecutionEntity> tasks = taskExecutionRepository.findAll();
            logger.info("Retrieved {} tasks", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("Error retrieving all tasks", e);
            throw e;
        }
    }

    @Override
    public Optional<TaskExecutionEntity> findById(String id) {
        try {
            Optional<TaskExecutionEntity> task = taskExecutionRepository.findById(id);
            logger.info("Task retrieval by ID: {}", task.isPresent() ? "Found" : "Not Found");
            return task;
        } catch (Exception e) {
            logger.error("Error retrieving task by ID: {}", id, e);
            throw e;
        }
    }

    @Override
    public TaskExecutionEntity save(TaskExecutionEntity task) {
        try {
            TaskExecutionEntity savedTask = taskExecutionRepository.save(task);
            logger.info("Task saved successfully with ID: {}", savedTask.getId());
            return savedTask;
        } catch (Exception e) {
            logger.error("Error saving task", e);
            throw e;
        }
    }

    // Keep the existing method for change stream listener
    public void updateTaskExecutionTime(
            String taskId,
            String status,
            java.time.Instant eventTime,
            String funnel,
            String applicationId,
            String entityId
    ) {
        try {
            logger.info("Updating task execution time for taskId: {}", taskId);
            // Implement your specific logic for updating task execution time
        } catch (Exception e) {
            logger.error("Error updating task execution time", e);
        }
    }
}