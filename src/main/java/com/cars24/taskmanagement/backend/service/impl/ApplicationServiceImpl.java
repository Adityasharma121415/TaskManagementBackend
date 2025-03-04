package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.TaskDetails;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;



@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationDao applicationDao;

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        List<TaskExecutionLog> tasks = applicationDao.findByApplicationId(applicationId);

        // Convert to TaskDetails and sort by updatedAt (oldest first)
        List<TaskDetails> sortedTasks = tasks.stream()
                .map(this::convertToTaskDetails)
                .sorted(Comparator.comparing(TaskDetails::getUpdatedAt))
                .collect(Collectors.toList());

        // Group consecutive tasks of the same funnel
        List<FunnelGroup> funnelGroups = groupTasksByFunnel(sortedTasks);

        TasksResponse response = new TasksResponse();
        response.setFunnelGroups(funnelGroups);
        return response;
    }

    private List<FunnelGroup> groupTasksByFunnel(List<TaskDetails> sortedTasks) {
        List<FunnelGroup> funnelGroups = new ArrayList<>();

        if (sortedTasks.isEmpty()) {
            return funnelGroups;
        }

        String currentFunnel = null;
        FunnelGroup currentGroup = null;

        for (TaskDetails task : sortedTasks) {
            String taskFunnel = task.getFunnel();

            // If this is a new funnel or the first task
            if (currentFunnel == null || !currentFunnel.equals(taskFunnel)) {
                currentFunnel = taskFunnel;
                currentGroup = new FunnelGroup();
                currentGroup.setFunnelName(taskFunnel);
                currentGroup.setTasks(new ArrayList<>());
                funnelGroups.add(currentGroup);
            }

            // Add task to the current funnel group
            currentGroup.getTasks().add(task);
        }

        return funnelGroups;
    }

    private TaskDetails convertToTaskDetails(TaskExecutionLog log) {
        TaskDetails details = new TaskDetails();
        details.setId(log.getId());
        details.setParentId(log.getParentId());
        details.setTaskId(log.getTaskId());
        details.setVersion(log.getVersion());
        details.setOrder(log.getOrder());
        details.setTemplateId(log.getTemplateId());
        details.setTemplateVersion(log.getTemplateVersion());
        details.setFunnel(log.getFunnel());
        details.setChannel(log.getChannel());
        details.setProductType(log.getProductType());
        details.setApplicationId(log.getApplicationId());
        details.setEntityIdentifier(log.getEntityIdentifier());
        details.setEntityType(log.getEntityType());
        details.setActorType(log.getActorType());
        details.setActorId(log.getActorId());
        details.setStatus(log.getStatus());
        details.setExecutionType(log.getExecutionType());
        details.setMetadata(log.getMetadata());
        details.setInputResourceValueMap(log.getInputResourceValueMap());
        details.setCreatedAt(log.getCreatedAt());
        details.setUpdatedAt(log.getUpdatedAt());
        details.setHandledBy(log.getHandledBy());
        return details;
    }
}


