package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.TaskDetails;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;




import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private TaskExecutionLogRepository taskExecutionLogRepository;

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        // Use the repository method with aggregation
        List<TaskExecutionLog> sortedTasks = taskExecutionLogRepository.findTasksByApplicationIdSortedByUpdatedAt(applicationId);

        // Convert to TaskDetails
        List<TaskDetails> taskDetailsList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());

        // Group consecutive tasks of the same funnel
        List<FunnelGroup> funnelGroups = groupTasksByFunnel(taskDetailsList);

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



