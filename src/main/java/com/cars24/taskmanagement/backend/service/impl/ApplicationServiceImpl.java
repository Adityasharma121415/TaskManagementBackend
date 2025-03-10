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
import org.springframework.http.MediaType;
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
        List<TaskExecutionLog> logs = applicationDao.findByApplicationId(applicationId);
        logger.info("Found {} task execution logs for application ID: {}", logs.size(), applicationId);

        if (logs.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ApplicationTasksResponse(Collections.emptyList()));
        }

        Map<String, List<TaskExecutionLog>> logsByFunnel = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getFunnel() != null ? log.getFunnel() : "UNKNOWN"));

        Map<String, Integer> funnelMinOrders = calculateFunnelMinOrders(logs);
        List<FunnelGroup> funnelGroups = buildFunnelGroups(logsByFunnel, funnelMinOrders);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationTasksResponse(funnelGroups));
    }

    private List<FunnelGroup> buildFunnelGroups(Map<String, List<TaskExecutionLog>> logsByFunnel,
                                                Map<String, Integer> funnelMinOrders) {
        List<FunnelGroup> funnelGroups = new ArrayList<>();

        funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String funnelName = entry.getKey();
                    FunnelGroup funnelGroup = new FunnelGroup();
                    funnelGroup.setFunnelName(funnelName);
                    funnelGroup.setOrder(entry.getValue());
                    funnelGroup.setTasks(processTasksInFunnel(logsByFunnel.get(funnelName)));
                    funnelGroups.add(funnelGroup);
                });

        return funnelGroups;
    }

    private List<TaskInfo> processTasksInFunnel(List<TaskExecutionLog> funnelLogs) {
        return funnelLogs.stream()
                .collect(Collectors.groupingBy(TaskExecutionLog::getTaskId))
                .entrySet().stream()
                .map(entry -> createTaskInfo(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(TaskInfo::getOrder))
                .collect(Collectors.toList());
    }

    private TaskInfo createTaskInfo(String taskId, List<TaskExecutionLog> taskLogs) {
        TaskExecutionLog firstLog = taskLogs.get(0);

        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setOrder(firstLog.getOrder());
        taskInfo.setHandledBy(firstLog.getHandledBy());
        taskInfo.setCreatedAt(firstLog.getCreatedAt());

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


