package com.cars24.taskmanagement.backend.service.impl;
import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.TaskDetails;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.StatusLogResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service

public class ApplicationServiceImpl implements ApplicationService {

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";

    @Autowired
    ApplicationDao taskExecutionDao;

//    @Autowired
//    public ApplicationServiceImpl(ApplicationDao taskExecutionDao) {
//        this.taskExecutionDao = taskExecutionDao;
//    }

    public Map<String, List<TaskResponse>> getTasksGroupedByFunnel(String applicationId) {
        // Fetch tasks and loan duration in one go
        Map<String, Object> data = taskExecutionDao.findTasksAndLoanDurationByApplicationId(applicationId);
        List<TaskExecutionLog> tasks = (List<TaskExecutionLog>) data.get("tasks");
        LoanDuration loanDuration = (LoanDuration) data.get("loanDuration");

        // Create a map for task metadata (duration, sendbacks, visited)
        Map<String, LoanDuration.Task> taskMetadata = Optional.ofNullable(loanDuration)
                .map(ld -> Stream.of(ld.getSourcing(), ld.getCredit(), ld.getConversion(), ld.getFulfillment())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(LoanDuration.Task::getTaskId, task -> task))
                ).orElse(Collections.emptyMap());

        // Group tasks by funnel -> taskId -> logs
        Map<String, Map<String, List<TaskExecutionLog>>> tasksByFunnelAndId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse("UNKNOWN"),
                        Collectors.groupingBy(TaskExecutionLog::getTaskId)
                ));

        // Sort funnels based on minimum order
        List<String> sortedFunnels = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse("UNKNOWN"),
                        Collectors.minBy(Comparator.comparingInt(TaskExecutionLog::getOrder))
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(o -> o.map(TaskExecutionLog::getOrder).orElse(Integer.MAX_VALUE))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Transform into final response
        LinkedHashMap<String, List<TaskResponse>> result = new LinkedHashMap<>();

        for (String funnel : sortedFunnels) {
            List<TaskResponse> funnelTasks = tasksByFunnelAndId.getOrDefault(funnel, Collections.emptyMap())
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparing(list -> list.get(0).getOrder())))
                    .map(entry -> {
                        List<TaskExecutionLog> logs = entry.getValue();
                        TaskExecutionLog firstLog = logs.get(0);

                        // Create status history sorted by updatedAt
                        List<StatusLogResponse> statusLogs = logs.stream()
                                .sorted(Comparator.comparing(TaskExecutionLog::getUpdatedAt))
                                .map(log -> new StatusLogResponse(log.getStatus(), log.getUpdatedAt()))
                                .collect(Collectors.toList());

                        // Create TaskResponse
                        TaskResponse taskResponse = new TaskResponse(
                                firstLog.getTaskId(),
                                firstLog.getOrder(),
                                firstLog.getHandledBy(),
                                firstLog.getCreatedAt(),
                                statusLogs
                        );

                        // Attach metadata if available
                        LoanDuration.Task metadata = taskMetadata.get(firstLog.getTaskId());
                        if (metadata != null) {
                            taskResponse.setDuration(metadata.getDuration());
                            taskResponse.setSendbacks(metadata.getSendbacks());
                            taskResponse.setVisited(metadata.getVisited());
                        }

                        return taskResponse;
                    })
                    .collect(Collectors.toList());

            result.put(funnel, funnelTasks);
        }

        return result;
    }

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        // Use the DAO to get sorted tasks from the repository
        List<TaskExecutionLog> sortedTasks = taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);

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

        details.setTaskId(log.getTaskId());

        // Handle null funnel in the conversion process
        details.setFunnel(log.getFunnel() != null ? log.getFunnel() : UNKNOWN_FUNNEL);

        details.setActorId(log.getActorId());
        details.setStatus(log.getStatus());

        details.setUpdatedAt(log.getUpdatedAt());
        details.setMetadata(log.getMetadata());

        return details;
    }
}