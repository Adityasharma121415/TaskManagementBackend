package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import com.cars24.taskmanagement.backend.service.changeStreams.TaskExecutionPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl implements TaskExecutionService {
    private final TaskExecutionTimeRepository taskExecutionTimeRepository;
    boolean flag = false;

    @Override
    public void updateTaskExecutionTime(
            String taskId, String status, Instant createdAt, Instant updatedAt,
            String funnel, String applicationId, String entityId, String channel
    ) {
        try {
            log.info("Updating task execution time for taskId: {}, funnel: {}, status: {}, channel: {}",
                    taskId, funnel, status, channel);

            TaskExecutionTimeEntity taskTimeEntity = taskExecutionTimeRepository
                    .findByApplicationIdAndEntityId(applicationId, entityId)
                    .orElseGet(() -> {
                        log.info("No existing TaskExecutionTimeEntity found for applicationId: {}, entityId: {}. Creating new entity.",
                                applicationId, entityId);
                        TaskExecutionTimeEntity newEntity = new TaskExecutionTimeEntity();
                        newEntity.setApplicationId(applicationId);
                        newEntity.setEntityId(entityId);
                        newEntity.setChannel(channel);
                        return taskExecutionTimeRepository.save(newEntity);
                    });

            taskTimeEntity.setChannel(channel);

            List<SubTaskEntity> subTaskEntityList;
            switch (funnel.toLowerCase()) {
                case "sourcing":
                    if (taskTimeEntity.getSourcing() == null) taskTimeEntity.setSourcing(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getSourcing();
                    break;
                case "credit":
                    if (taskTimeEntity.getCredit() == null) taskTimeEntity.setCredit(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getCredit();
                    break;
                case "conversion":
                    if (taskTimeEntity.getConversion() == null) taskTimeEntity.setConversion(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getConversion();
                    break;
                case "fulfilment":
                    if (taskTimeEntity.getFulfillment() == null) taskTimeEntity.setFulfillment(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getFulfillment();
                    break;
                case "risk":
                    if (taskTimeEntity.getRisk() == null) taskTimeEntity.setRisk(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getRisk();
                    break;
                case "rto":
                    if (taskTimeEntity.getRto() == null) taskTimeEntity.setRto(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getRto();
                    break;
                case "disbursal":
                    if (taskTimeEntity.getDisbursal() == null) taskTimeEntity.setDisbursal(new ArrayList<>());
                    subTaskEntityList = taskTimeEntity.getDisbursal();
                    break;
                default:
                    log.warn("Unknown funnel type: {}. Task execution time update skipped.", funnel);
                    return;
            }

            SubTaskEntity subTaskEntity = subTaskEntityList.stream()
                    .filter(t -> t.getTaskId().equals(taskId))
                    .findFirst()
                    .orElseGet(() -> {
                        log.info("Creating new subtask for taskId: {}", taskId);
                        SubTaskEntity newSubTaskEntity = new SubTaskEntity(taskId, createdAt);
                        subTaskEntityList.add(newSubTaskEntity);
                        return newSubTaskEntity;
                    });

            subTaskEntity.updateStatus(status, updatedAt, flag);
            if (flag) {
                flag = false;
            }
            if (status.equalsIgnoreCase("COMPLETED") && taskId.equalsIgnoreCase("sendback")) {
                flag = true;
            }

            taskTimeEntity.setRecordDate(updatedAt);
            taskExecutionTimeRepository.save(taskTimeEntity);

            log.info("Task execution time updated successfully for applicationId={}, entityId={}, taskId={}, channel={}",
                    applicationId, entityId, taskId, channel);

        } catch (Exception e) {
            log.error("Error updating task execution time for taskId: {}, funnel: {}, status: {}, channel={}",
                    taskId, funnel, status, channel, e);
        }
    }

    // New RabbitMQ listener that consumes payloads from the queue.
    @RabbitListener(queues = "taskPriorityQueue")
    public void processTaskExecutionPayload(TaskExecutionPayload payload) {
        log.info("Received payload from RabbitMQ: TaskId={}, Status={}", payload.getTaskId(), payload.getStatus());
        updateTaskExecutionTime(
                payload.getTaskId(),
                payload.getStatus(),
                payload.getCreatedAt(),
                payload.getUpdatedAt(),
                payload.getFunnel(),
                payload.getApplicationId(),
                payload.getEntityId(),
                payload.getChannel()
        );
    }
}
