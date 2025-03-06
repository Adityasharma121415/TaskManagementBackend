package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionRepository;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl implements TaskExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionServiceImpl.class);

    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskExecutionTimeRepository taskExecutionTimeRepository;

    @Override
    public List<TaskExecutionEntity> findAll() {
        try {
            List<TaskExecutionEntity> tasks = taskExecutionRepository.findAll();
            logger.info("Retrieved {} task executions", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("Error retrieving all task executions", e);
            throw e;
        }
    }

    @Override
    public Optional<TaskExecutionEntity> findById(String id) {
        try {
            Optional<TaskExecutionEntity> task = taskExecutionRepository.findById(id);
            logger.info("Task retrieval by ID {}: {}", id, task.isPresent() ? "Found" : "Not Found");
            return task;
        } catch (Exception e) {
            logger.error("Error retrieving task execution by ID: {}", id, e);
            throw e;
        }
    }

    @Override
    public TaskExecutionEntity save(TaskExecutionEntity task) {
        try {
            TaskExecutionEntity savedTask = taskExecutionRepository.save(task);
            logger.info("Task execution saved successfully with ID: {}", savedTask.getId());
            return savedTask;
        } catch (Exception e) {
            logger.error("Error saving task execution", e);
            throw e;
        }
    }

    @Override
    public void updateTaskExecutionTime(
            String taskId, String status, Instant eventTime,
            String funnel, String applicationId, String entityId
    ) {
        try {
            logger.info("Updating task execution time for taskId: {}, funnel: {}", taskId, funnel);

            // Fetch or create TaskExecutionTimeEntity
            Optional<TaskExecutionTimeEntity> optionalTaskTime =
                    taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId);

            TaskExecutionTimeEntity taskTimeEntity = optionalTaskTime.orElseGet(() -> {
                logger.info("No existing TaskExecutionTimeEntity found for applicationId: {}, entityId: {}. Creating new entity.", applicationId, entityId);
                TaskExecutionTimeEntity newEntity = new TaskExecutionTimeEntity();
                newEntity.setApplicationId(applicationId);
                newEntity.setEntityId(entityId);
                return newEntity;
            });

            // Create SubTask entry
            SubTaskEntity subTaskEntity = new SubTaskEntity(taskId, status, eventTime);

            // Add subTask to the appropriate funnel list
            switch (funnel.toLowerCase()) {
                case "sourcing":
                    taskTimeEntity.getSourcing().add(subTaskEntity);
                    break;
                case "credit":
                    taskTimeEntity.getCredit().add(subTaskEntity);
                    break;
                case "conversion":
                    taskTimeEntity.getConversion().add(subTaskEntity);
                    break;
                case "fulfillment":
                    taskTimeEntity.getFulfillment().add(subTaskEntity);
                    break;
                default:
                    logger.warn("Unknown funnel type: {}. Task execution time update skipped.", funnel);
                    return;
            }

            // Save updated entity
            taskExecutionTimeRepository.save(taskTimeEntity);
            logger.info("Task execution time updated successfully for applicationId={}, entityId={}",
                    applicationId, entityId);

        } catch (Exception e) {
            logger.error("Error updating task execution time for taskId: {}, funnel: {}", taskId, funnel, e);
        }
    }
}
