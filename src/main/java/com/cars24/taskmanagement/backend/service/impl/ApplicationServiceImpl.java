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

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";

    @Autowired
    private ApplicationDao applicationDao;

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        // Use the DAO to get sorted tasks from the repository
        List<TaskExecutionLog> sortedTasks = applicationDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);

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
            // Handle null funnel by replacing with "Unknown Funnel"
            String taskFunnel = (task.getFunnel() != null) ? task.getFunnel() : UNKNOWN_FUNNEL;

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

        details.setTaskId(log.getTaskId());


        // Handle null funnel in the conversion process
        details.setFunnel(log.getFunnel() != null ? log.getFunnel() : UNKNOWN_FUNNEL);


        details.setEntityIdentifier(log.getEntityIdentifier());

        details.setActorId(log.getActorId());
        details.setStatus(log.getStatus());

        details.setUpdatedAt(log.getUpdatedAt());

        return details;
    }
}



