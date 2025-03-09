package com.cars24.taskmanagement.backend.service.impl;


import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.exception.SlaResourceNotFoundException;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlaServiceImpl {

    private final SlaDaoImpl slaDao;

    public SlaServiceImpl(SlaDaoImpl slaDao) {
        this.slaDao = slaDao;
    }

    public Map<String, Double> calculateAverageTimePerFunnel() {
        List<TaskExecutionTimeEntity> executions = slaDao.getAllTasks();
        if (executions.isEmpty()) {
            throw new SlaResourceNotFoundException("No task execution data found");
        }

        Map<String, List<Long>> funnelDurations = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = Map.of(
                    "sourcing", execution.getSourcing(),
                    "credit", execution.getCredit(),
                    "conversion", execution.getConversion(),
                    "fulfillment", execution.getFulfillment()
            );

            funnels.forEach((funnelName, tasks) -> {
                long totalDuration = tasks.stream().mapToLong(SubTaskEntity::getDuration).sum();
                funnelDurations.computeIfAbsent(funnelName, k -> new ArrayList<>()).add(totalDuration);
            });
        }

        return funnelDurations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)
                ));
    }

    // 📌 Average time per task
    public Map<String, Double> calculateAverageTimePerTask() {
        List<TaskExecutionTimeEntity> executions = slaDao.getAllTasks();
        if (executions.isEmpty()) {
            throw new SlaResourceNotFoundException("No task execution data found");
        }

        Map<String, List<Long>> taskDurations = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            List<SubTaskEntity> allTasks = new ArrayList<>();
            allTasks.addAll(execution.getSourcing());
            allTasks.addAll(execution.getCredit());
            allTasks.addAll(execution.getConversion());
            allTasks.addAll(execution.getFulfillment());

            for (SubTaskEntity task : allTasks) {
                taskDurations.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add(task.getDuration());
            }
        }

        return taskDurations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)
                ));
    }

    // 📌 Sendback frequency per task
    public Map<String, Long> calculateSendbackFrequency() {
        List<TaskExecutionTimeEntity> executions = slaDao.getAllTasks();
        if (executions.isEmpty()) {
            throw new SlaResourceNotFoundException("No task execution data found");
        }

        Map<String, Long> sendbackCounts = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            List<SubTaskEntity> allTasks = new ArrayList<>();
            allTasks.addAll(execution.getSourcing());
            allTasks.addAll(execution.getCredit());
            allTasks.addAll(execution.getConversion());
            allTasks.addAll(execution.getFulfillment());

            for (SubTaskEntity task : allTasks) {
                if (task.getSendback_time() != null) {
                    sendbackCounts.merge(task.getTaskId(), 1L, Long::sum);
                }
            }
        }

        return sendbackCounts;
    }
}
