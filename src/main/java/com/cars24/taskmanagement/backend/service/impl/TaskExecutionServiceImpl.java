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
            // Ensure updatedAt is set
            if (task.getUpdatedAt() == null) {
                task.setUpdatedAt(new java.util.Date());
            }

            TaskExecutionEntity savedTask = taskExecutionRepository.save(task);
            logger.info("Task execution saved successfully with ID: {}", savedTask.getId());

            // Manually trigger the time tracking for new tasks
            if ("NEW".equalsIgnoreCase(task.getStatus())) {
                updateTaskExecutionTime(
                        task.getTaskId(),
                        task.getStatus(),
                        task.getUpdatedAt().toInstant(),
                        task.getFunnel(),
                        task.getApplicationId(),
                        task.getEntityId()
                );
            }

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
            logger.info("Updating task execution time for taskId: {}, status: {}, funnel: {}",
                    taskId, status, funnel);

            // Fetch or create TaskExecutionTimeEntity
            Optional<TaskExecutionTimeEntity> optionalTaskTime =
                    taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId);

            TaskExecutionTimeEntity taskTimeEntity;
            if (optionalTaskTime.isPresent()) {
                taskTimeEntity = optionalTaskTime.get();
                logger.info("Found existing TaskExecutionTimeEntity for applicationId: {}, entityId: {}",
                        applicationId, entityId);
            } else {
                logger.info("Creating new TaskExecutionTimeEntity for applicationId: {}, entityId: {}",
                        applicationId, entityId);
                taskTimeEntity = new TaskExecutionTimeEntity();
                taskTimeEntity.setApplicationId(applicationId);
                taskTimeEntity.setEntityId(entityId);
                taskTimeEntity.setSourcing(new ArrayList<>());
                taskTimeEntity.setCredit(new ArrayList<>());
                taskTimeEntity.setConversion(new ArrayList<>());
                taskTimeEntity.setFulfillment(new ArrayList<>());
            }

            // Get the appropriate funnel list
            List<SubTaskEntity> funnelList;
            switch (funnel.toLowerCase()) {
                case "sourcing":
                    funnelList = taskTimeEntity.getSourcing();
                    break;
                case "credit":
                    funnelList = taskTimeEntity.getCredit();
                    break;
                case "conversion":
                    funnelList = taskTimeEntity.getConversion();
                    break;
                case "fulfillment":
                    funnelList = taskTimeEntity.getFulfillment();
                    break;
                default:
                    logger.warn("Unknown funnel type: {}. Update skipped.", funnel);
                    return;
            }

            // Find existing subtask or create new one
            SubTaskEntity subTask = null;
            for (SubTaskEntity task : funnelList) {
                if (task.getTaskId().equals(taskId)) {
                    subTask = task;
                    break;
                }
            }

            if (subTask == null) {
                // Create new subtask if it doesn't exist
                logger.info("Creating new subtask for taskId: {} in funnel: {}", taskId, funnel);
                subTask = new SubTaskEntity();
                subTask.setTaskId(taskId);
                subTask.setStatus("New");
                subTask.setDuration(0);
                subTask.setVisited(0);

                if ("NEW".equalsIgnoreCase(status)) {
                    subTask.setNewTime(eventTime);
                }

                funnelList.add(subTask);
            } else {
                // Update existing subtask based on status
                logger.info("Updating existing subtask for taskId: {} with status: {}", taskId, status);
                updateSubtaskStatus(subTask, status, eventTime);
            }

            // Save updated entity
            TaskExecutionTimeEntity savedEntity = taskExecutionTimeRepository.save(taskTimeEntity);
            logger.info("Task execution time updated successfully with ID: {}", savedEntity.getId());

        } catch (Exception e) {
            logger.error("Error updating task execution time for taskId: {}, funnel: {}",
                    taskId, funnel, e);
        }
    }

    private void updateSubtaskStatus(SubTaskEntity subTask, String status, Instant eventTime) {
        logger.info("Updating subtask status from {} to {}", subTask.getStatus(), status);

        switch (status.toUpperCase()) {
            case "NEW":
                // If the task was already created, we don't update anything
                if (subTask.getNewTime() == null) {
                    subTask.setNewTime(eventTime);
                    subTask.setStatus("New");
                }
                break;

            case "TODO":
            case "IN_PROGRESS":
                // Task moves to TODO/In Progress
                subTask.setTodoTime(eventTime);
                subTask.setStatus("In Progress");
                subTask.setVisited(subTask.getVisited() + 1);
                logger.info("Task moved to In Progress, visited count: {}", subTask.getVisited());
                break;

            case "COMPLETED":
                // Task moves to COMPLETED
                subTask.setCompletedTime(eventTime);
                subTask.setStatus("Completed");

                // Calculate duration
                if (subTask.getVisited() == 1) {
                    // First completion: duration = completedTime - newTime
                    if (subTask.getNewTime() != null) {
                        long durationMs = subTask.getCompletedTime().toEpochMilli() -
                                subTask.getNewTime().toEpochMilli();
                        subTask.setDuration(durationMs / 1000); // Convert to seconds
                    }
                } else {
                    // Subsequent completion: duration += completedTime - todoTime
                    if (subTask.getTodoTime() != null) {
                        long additionalDurationMs = subTask.getCompletedTime().toEpochMilli() -
                                subTask.getTodoTime().toEpochMilli();
                        subTask.setDuration(subTask.getDuration() + (additionalDurationMs / 1000));
                    }
                }

                logger.info("Task completed, total duration: {} seconds", subTask.getDuration());
                break;

            case "SENDBACK":
            case "SENT_BACK":
                // Task is sent back
                subTask.setSendbackTime(eventTime);
                subTask.setStatus("Sent Back");

                // Update duration when sent back
                if (subTask.getCompletedTime() != null) {
                    long additionalDurationMs = subTask.getSendbackTime().toEpochMilli() -
                            subTask.getCompletedTime().toEpochMilli();
                    subTask.setDuration(subTask.getDuration() + (additionalDurationMs / 1000));
                }

                logger.info("Task sent back, total duration: {} seconds", subTask.getDuration());
                break;

            case "FAILED":
                // Task fails
                subTask.setStatus("Failed");
                subTask.setVisited(Math.max(0, subTask.getVisited() - 1));
                logger.info("Task failed, visited count: {}", subTask.getVisited());
                break;

            default:
                logger.warn("Unknown task status: {}. No update performed.", status);
        }
    }
}