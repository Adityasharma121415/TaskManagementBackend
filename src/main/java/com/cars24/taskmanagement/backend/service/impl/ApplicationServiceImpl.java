package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.ApplicationTasksResponse;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.StatusUpdate;
import com.cars24.taskmanagement.backend.data.response.TaskInfo;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private final ApplicationDao applicationDao;

    @Autowired
    public ApplicationServiceImpl(ApplicationDao applicationDao) {
        this.applicationDao = applicationDao;
    }

    @Override
    public ResponseEntity<ApplicationTasksResponse> getTasksGroupedByFunnel(String applicationId) {
        logger.debug("Fetching tasks for application ID: {}", applicationId);

        try {
            List<TaskExecutionLog> logs = applicationDao.findByApplicationId(applicationId);
           logger.info("Found {} task execution logs for application ID: {}", logs.size(), applicationId);

            if (logs.isEmpty()) {
                logger.warn("No tasks found for application ID: {}", applicationId);
                return ResponseEntity.ok(new ApplicationTasksResponse(Collections.emptyList()));
            }

            // Group logs by funnel
            Map<String, List<TaskExecutionLog>> logsByFunnel = logs.stream()
                    .collect(Collectors.groupingBy(
                            log -> log.getFunnel() != null ? log.getFunnel() : "UNKNOWN"
                    ));

            // Calculate minimum order for each funnel (for sorting funnels)
            Map<String, Integer> funnelMinOrders = calculateFunnelMinOrders(logs);

            // Create and populate funnel groups
            List<FunnelGroup> funnelGroups = new ArrayList<>();

            funnelMinOrders.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> {
                        String funnelName = entry.getKey();
                        int funnelOrder = entry.getValue();
                        List<TaskExecutionLog> funnelLogs = logsByFunnel.get(funnelName);

                        FunnelGroup funnelGroup = new FunnelGroup();
                        funnelGroup.setFunnelName(funnelName);
                        funnelGroup.setOrder(funnelOrder);
                        funnelGroup.setTasks(processTasksInFunnel(funnelLogs));

                        funnelGroups.add(funnelGroup);
                    });

            ApplicationTasksResponse response = new ApplicationTasksResponse(funnelGroups);
            return ResponseEntity.ok(response);

        } catch (DataAccessException e) {
            //logger.error("Data access error while fetching tasks for application ID: {}", applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApplicationTasksResponse(e.getMessage()));
        } catch (Exception e) {
            //logger.error("Unexpected error while fetching tasks for application ID: {}", applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApplicationTasksResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    private List<TaskInfo> processTasksInFunnel(List<TaskExecutionLog> funnelLogs) {
        // Group logs by taskId
        Map<String, List<TaskExecutionLog>> logsByTaskId = funnelLogs.stream()
                .collect(Collectors.groupingBy(TaskExecutionLog::getTaskId));

        // Create TaskInfo objects for each task
        List<TaskInfo> tasks = new ArrayList<>();

        logsByTaskId.forEach((taskId, taskLogs) -> {
            TaskInfo taskInfo = createTaskInfo(taskId, taskLogs);
            tasks.add(taskInfo);
        });

        // Sort tasks by order
        tasks.sort(Comparator.comparingInt(TaskInfo::getOrder));

        return tasks;
    }

    private TaskInfo createTaskInfo(String taskId, List<TaskExecutionLog> taskLogs) {
        // Get first log for task details
        TaskExecutionLog firstLog = taskLogs.get(0);

        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setOrder(firstLog.getOrder());
        taskInfo.setHandledBy(firstLog.getHandledBy());
        taskInfo.setCreatedAt(firstLog.getCreatedAt());

        // Create status history
        List<StatusUpdate> statusUpdates = taskLogs.stream()
                .sorted(Comparator.comparing(TaskExecutionLog::getUpdatedAt))
                .map(log -> {
                    StatusUpdate update = new StatusUpdate();
                    update.setStatus(log.getStatus());
                    update.setUpdatedAt(log.getUpdatedAt());
                    return update;
                })
                .collect(Collectors.toList());

        taskInfo.setStatusHistory(statusUpdates);

        return taskInfo;
    }

    private Map<String, Integer> calculateFunnelMinOrders(List<TaskExecutionLog> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getFunnel() != null ? log.getFunnel() : "UNKNOWN",
                        Collectors.mapping(TaskExecutionLog::getOrder, Collectors.minBy(Integer::compare))))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));
    }
}


