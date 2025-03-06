package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.SubTask;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionRepository;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl implements TaskExecutionService {
    @Autowired
    private final TaskExecutionRepository taskExecutionRepository;
    @Autowired
    private final TaskExecutionTimeRepository taskExecutionTimeRepository;

    @Override
    public List<TaskExecutionEntity> findAll() {
        return taskExecutionRepository.findAll();
    }

    @Override
    public Optional<TaskExecutionEntity> findById(String id) {
        return taskExecutionRepository.findById(id);
    }

    @Override
    public TaskExecutionEntity save(TaskExecutionEntity task) {
        TaskExecutionEntity savedTask = taskExecutionRepository.save(task);
        processTaskExecution(savedTask);
        return savedTask;
    }

    @Override
    public void processTaskExecution(TaskExecutionEntity task) {
        String applicationId = task.getApplicationId();
        String entityId = task.getEntityId();
        String funnel = task.getFunnel();
        String status = task.getStatus();
        Instant currentTime = Instant.now();

        Optional<TaskExecutionTimeEntity> existingRecord =
                taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId);

        TaskExecutionTimeEntity taskExecutionTime;
        if (existingRecord.isPresent()) {
            taskExecutionTime = existingRecord.get();
        } else {
            taskExecutionTime = new TaskExecutionTimeEntity();
            taskExecutionTime.setApplicationId(applicationId);
            taskExecutionTime.setEntityId(entityId);
        }

        List<SubTask> subTaskList;
        switch (funnel) {
            case "SOURCING":
                subTaskList = taskExecutionTime.getSourcing();
                break;
            case "CREDIT":
                subTaskList = taskExecutionTime.getCredit();
                break;
            case "CONVERSION":
                subTaskList = taskExecutionTime.getConversion();
                break;
            case "FULFILLMENT":
                subTaskList = taskExecutionTime.getFulfillment();
                break;
            default:
                throw new IllegalArgumentException("Invalid funnel: " + funnel);
        }

        // Find if the task already exists in the funnel
        SubTask subTask = subTaskList.stream()
                .filter(t -> t.getTaskId().equals(task.getTaskId()))
                .findFirst()
                .orElse(null);

        if (subTask == null) {
            // New Task
            subTask = new SubTask();
            subTask.setTaskId(task.getTaskId());
      //    subTask.setNewTime(task.get()); // Use createdAt from task_execution
            subTask.setDuration(0);
            subTask.setVisited(0);
            subTaskList.add(subTask);
        }

        // Handle Task Status Updates
        switch (status) {
            case "TODO":
                subTask.setTodoTime(currentTime);
                subTask.setVisited(subTask.getVisited() + 1);
                break;

            case "COMPLETED":
                subTask.setCompletedTime(currentTime);
                if (subTask.getVisited() == 1) {
                    subTask.setDuration(subTask.getDuration() + Duration.between(subTask.getNewTime(), subTask.getCompletedTime()).toSeconds());
                } else {
                    subTask.setDuration(subTask.getDuration() + Duration.between(subTask.getTodoTime(), subTask.getCompletedTime()).toSeconds());
                }
                break;

            case "SENT_BACK":
                subTask.setSendbackTime(currentTime);
                if (subTask.getVisited() == 1) {
                    subTask.setDuration(subTask.getDuration() + Duration.between(subTask.getNewTime(), subTask.getSendbackTime()).toSeconds());
                } else {
                    subTask.setDuration(subTask.getDuration() + Duration.between(subTask.getTodoTime(), subTask.getSendbackTime()).toSeconds());
                }
                break;

            case "FAILED":
                if (subTask.getVisited() > 0) {
                    subTask.setVisited(subTask.getVisited() - 1);
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid task status: " + status);
        }

        taskExecutionTimeRepository.save(taskExecutionTime);
    }
}
