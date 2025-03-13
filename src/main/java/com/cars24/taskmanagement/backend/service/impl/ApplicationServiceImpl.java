package com.cars24.taskmanagement.backend.service.impl;
import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.TaskDetails;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.StatusLogResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";
    private final ApplicationDao taskExecutionDao;
    private final SendbackConfigDao sendbackConfigDao;

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        List<TaskExecutionLog> sortedTasks = taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
        List<TaskDetails> taskDetailsList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());
        List<FunnelGroup> funnelGroups = groupTasksByFunnel(taskDetailsList);
        return new TasksResponse(funnelGroups);
    }

    @Override
    public Map<String, List<TaskResponse>> getTasksGroupedByFunnel(String applicationId) {
        Map<String, Object> data = taskExecutionDao.findTasksAndLoanDurationByApplicationId(applicationId);
        List<TaskExecutionLog> tasks = (List<TaskExecutionLog>) data.getOrDefault("tasks", Collections.emptyList());
        LoanDuration loanDuration = (LoanDuration) data.get("loanDuration");

        Map<String, Integer> funnelMinOrders = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.mapping(TaskExecutionLog::getOrder, Collectors.minBy(Integer::compare))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));

        Map<String, LoanDuration.Task> taskMetadata = Optional.ofNullable(loanDuration)
                .map(ld -> Stream.of(ld.getSourcing(), ld.getCredit(), ld.getConversion(), ld.getFulfillment())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(LoanDuration.Task::getTaskId, task -> task, (a, b) -> a))
                ).orElse(Collections.emptyMap());

        Map<String, Map<String, List<TaskExecutionLog>>> tasksByFunnelAndId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.groupingBy(TaskExecutionLog::getTaskId)
                ));

        List<String> sortedFunnels = funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();

        LinkedHashMap<String, List<TaskResponse>> result = new LinkedHashMap<>();

        for (String funnel : sortedFunnels) {
            List<TaskResponse> funnelTasks = tasksByFunnelAndId.getOrDefault(funnel, Collections.emptyMap())
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparing(list -> list.getFirst().getOrder())))
                    .map(entry -> createTaskResponse(entry.getValue(), taskMetadata))
                    .collect(Collectors.toList());

            result.put(funnel, funnelTasks);
        }
        return result;
    }

    private TaskResponse createTaskResponse(List<TaskExecutionLog> logs, Map<String, LoanDuration.Task> taskMetadata) {
        TaskExecutionLog firstLog = logs.getFirst();

        List<StatusLogResponse> statusLogs = logs.stream()
                .sorted(Comparator.comparing(TaskExecutionLog::getUpdatedAt))
                .map(log -> new StatusLogResponse(log.getStatus(), log.getUpdatedAt()))
                .collect(Collectors.toList());

        LoanDuration.Task metadata = taskMetadata.get(firstLog.getTaskId());
        long duration = metadata != null ? metadata.getDuration() : 0;
        int sendbacks = metadata != null ? metadata.getSendbacks() : 0;
        int visited = metadata != null ? metadata.getVisited() : 0;

        String targetTaskId = null;
        if ("sendback".equalsIgnoreCase(firstLog.getTaskId())) {
            targetTaskId = fetchTargetTaskId(firstLog);
        }

        return new TaskResponse(
                firstLog.getTaskId(),
                firstLog.getOrder(),
                firstLog.getHandledBy(),
                firstLog.getCreatedAt(),
                statusLogs,
                targetTaskId,
                duration,
                sendbacks,
                visited
        );
    }

    private String fetchTargetTaskId(TaskExecutionLog log) {
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getMetadata().get("sendbackMetadata");
        if (sendbackMetadata != null && sendbackMetadata.containsKey("key")) {
            String sendbackKey = (String) sendbackMetadata.get("key");
            return sendbackConfigDao.findBySendbackKey(sendbackKey)
                    .map(config -> !config.getSubReasonList().isEmpty() ? config.getSubReasonList().get(0).getTargetTaskId() : null)
                    .orElse(null);
        }
        return null;
    }

    private List<FunnelGroup> groupTasksByFunnel(List<TaskDetails> sortedTasks) {
        List<FunnelGroup> funnelGroups = new ArrayList<>();
        if (sortedTasks.isEmpty()) return funnelGroups;

        Map<String, FunnelGroup> funnelMap = new LinkedHashMap<>();
        for (TaskDetails task : sortedTasks) {
            String taskFunnel = Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL);
            funnelMap.computeIfAbsent(taskFunnel, key -> new FunnelGroup(taskFunnel, new ArrayList<>()))
                    .getTasks().add(task);
        }
        return new ArrayList<>(funnelMap.values());
    }

    private TaskDetails convertToTaskDetails(TaskExecutionLog log) {
        String targetTaskId = "sendback".equalsIgnoreCase(log.getTaskId()) ? fetchTargetTaskId(log) : null;

        return new TaskDetails(
                Optional.ofNullable(log.getFunnel()).orElse(UNKNOWN_FUNNEL), // 1: Funnel
                log.getActorId(),
                log.getStatus(),
                log.getUpdatedAt(),
                log.getTaskId(),
                targetTaskId,
                0,
                0,
                log.getMetadata()
        );
    }

}
