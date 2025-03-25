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
import java.util.ArrayList;
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
    public void updateTaskExecutionTime(String taskId, String status, Instant eventTime, String funnel, String applicationId, String entityId) {

    }

    @Override
    public void updateTaskExecutionTime(String taskId, String status, Instant createdAt, Instant updatedAt, String funnel, String applicationId, String entityId) {

    }

    @Override
    public void updateTaskExecutionTime(
            String taskId, String status, Instant createdAt, Instant updatedAt,
            String funnel, String applicationId, String entityId, String channel
    ) {
        try {
            logger.info("Updating task execution time for taskId: {}, funnel: {}, status: {}, channel: {}",
                    taskId, funnel, status, channel);

            // First, try to fetch an existing TaskExecutionTimeEntity.
            TaskExecutionTimeEntity taskTimeEntity = taskExecutionTimeRepository
                    .findByApplicationIdAndEntityId(applicationId, entityId)
                    .orElse(null);

            // If no record exists, try to create a new one.
            if (taskTimeEntity == null) {
                TaskExecutionTimeEntity newEntity = new TaskExecutionTimeEntity();
                newEntity.setApplicationId(applicationId);
                newEntity.setEntityId(entityId);
                newEntity.setChannel(channel);
                try {
                    taskTimeEntity = taskExecutionTimeRepository.save(newEntity);
                } catch (org.springframework.dao.DuplicateKeyException ex) {

                    taskTimeEntity = taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId)
                            .orElseThrow(() -> new RuntimeException("Failed to retrieve duplicate record", ex));
                }
            } else {

                taskTimeEntity.setChannel(channel);
            }


            List<SubTaskEntity> subTaskEntityList;
            switch (funnel.toLowerCase()) {
                case "sourcing":
                    if (taskTimeEntity.getSourcing() == null) {
                        taskTimeEntity.setSourcing(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getSourcing();
                    break;
                case "credit":
                    if (taskTimeEntity.getCredit() == null) {
                        taskTimeEntity.setCredit(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getCredit();
                    break;
                case "conversion":
                    if (taskTimeEntity.getConversion() == null) {
                        taskTimeEntity.setConversion(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getConversion();
                    break;
                case "fulfillment":
                    if (taskTimeEntity.getFulfillment() == null) {
                        taskTimeEntity.setFulfillment(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getFulfillment();
                    break;
                case "risk":
                    if (taskTimeEntity.getRisk() == null) {
                        taskTimeEntity.setRisk(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getRisk();
                    break;
                case "rto":
                    if (taskTimeEntity.getRto() == null) {
                        taskTimeEntity.setRto(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getRto();
                    break;
                case "disbursal":
                    if (taskTimeEntity.getDisbursal() == null) {
                        taskTimeEntity.setDisbursal(new ArrayList<>());
                    }
                    subTaskEntityList = taskTimeEntity.getDisbursal();
                    break;
                default:
                    logger.warn("Unknown funnel type: {}. Task execution time update skipped.", funnel);
                    return;
            }


            SubTaskEntity subTaskEntity = subTaskEntityList.stream()
                    .filter(t -> t.getTaskId().equals(taskId))
                    .findFirst()
                    .orElseGet(() -> {
                        logger.info("Creating new subtask for taskId: {}", taskId);
                        SubTaskEntity newSubTaskEntity = new SubTaskEntity(taskId, createdAt);
                        subTaskEntityList.add(newSubTaskEntity);
                        return newSubTaskEntity;
                    });


            subTaskEntity.updateStatus(status, updatedAt);

            taskTimeEntity.setRecordDate(updatedAt);


            taskExecutionTimeRepository.save(taskTimeEntity);

            logger.info("Task execution time updated successfully for applicationId={}, entityId={}, taskId={}, channel={}",
                    applicationId, entityId, taskId, channel);

        } catch (Exception e) {
            logger.error("Error updating task execution time for taskId: {}, funnel: {}, status: {}, channel: {}",
                    taskId, funnel, status, channel, e);
        }
    }


}
