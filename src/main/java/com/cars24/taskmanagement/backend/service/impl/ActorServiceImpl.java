package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.service.ActorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorServiceImpl implements ActorService {

    private final ActorDaoImpl actorDao;

    private List<ActorEntity> actorDocuments = new ArrayList<>();

    private List<ActorEntity> allDocuments = new ArrayList<>();

    @Override
    public void getApplications(String actorId, int days) {
        log.info("ActorServiceImpl [getApplications] {} {}", actorId, days);
        Date pastDate = getPastDate(days);
        actorDocuments = actorDao.findAllByActorIdAndLastUpdatedAtAfter(actorId, pastDate);
    }

    @Override
    public void getAllApplications(String actorType, int days){
        log.info("ActorServiceImpl [getAllApplications] {} {}",actorType, days);
        Date pastDate = getPastDate(days);
        allDocuments = actorDao.findAllApplications(actorType, pastDate);
    }

    @Override
    public double getTaskEffiencyScore(String actorId) {
        log.info("ActorServiceImpl [getTaskEfficiencyScore] {}", actorId);

        Map<String, Double> averageTaskTimeForAllAgents = thresholdAverageTaskTime();
        Map<String, Double> averageTaskTimeForAgent = getAverageTaskTime(actorId);

        int efficientTasks = 0;
        int totalNoOfTasks = 0;

        for(Map.Entry<String, Double> entry : averageTaskTimeForAgent.entrySet()){
            String taskId = entry.getKey();
            Double agentTaskTime = entry.getValue();

            Double averageTaskTime = averageTaskTimeForAllAgents.get(taskId);

            if(agentTaskTime < averageTaskTime){
                efficientTasks += 1;
            }
            totalNoOfTasks += 1;
        }

        Double score = 0.0;
        if(totalNoOfTasks > 0){
            score = (efficientTasks * 1.0 / totalNoOfTasks) * 100;
        }
        return score;
    }

    @Override
    public Map<String, Object> getFastestAndSlowestTask(String actorId) {
        log.info("ActorServiceImpl [getFastestAndSlowestTask] {}", actorId);

        TaskEntity fastestTask = null;
        TaskEntity slowestTask = null;

        for(ActorEntity document : actorDocuments){
            if(!document.getActorId().equals(actorId)) continue;

            for(TaskEntity task : document.getTasks()){
                if(fastestTask == null || task.getDuration() < fastestTask.getDuration()){
                    fastestTask = task;
                }
                if(slowestTask == null || task.getDuration() > slowestTask.getDuration()){
                    slowestTask = task;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        if(fastestTask != null){
            result.put("fastest_task", Map.of("task_id", fastestTask.getTaskId(), "duration", fastestTask.getDuration()));
        }
        if(slowestTask != null){
            result.put("slowest_task", Map.of("task_id", slowestTask.getTaskId(), "duration", slowestTask.getDuration()));
        }
        return result;
    }

    @Override
    public Map<String, Object> getMostAndLeastRetriedTask(String actorId) {
        log.info("ActorServiceImpl [getMostAndLeastRetriedTsak] {}", actorId);

        TaskEntity mostRetriedTask = null;
        TaskEntity leastRetriedTask = null;

        for(ActorEntity document : actorDocuments){
            if(!document.getActorId().equals(actorId)) continue;

            for(TaskEntity task : document.getTasks()){
                if(mostRetriedTask == null || task.getVisited() > mostRetriedTask.getVisited()){
                    mostRetriedTask = task;
                }
                if(leastRetriedTask == null || task.getVisited() < leastRetriedTask.getVisited()){
                    leastRetriedTask = task;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        if(mostRetriedTask != null){
            int mostVisited = mostRetriedTask.getVisited() > 0 ? mostRetriedTask.getVisited() - 1: 0;
            result.put("most_retried_task", Map.of("task_id", mostRetriedTask.getTaskId(), "visited", mostVisited));
        }
        if(leastRetriedTask != null){
            int leastVisited = leastRetriedTask.getVisited() > 0 ? leastRetriedTask.getVisited() - 1 : 0;
            result.put("least_retried_task", Map.of("task_id", leastRetriedTask.getTaskId(), "visited", leastVisited));
        }
        return result;
    }

    @Override
    public Date getPastDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }

    @Override
    public String getActorEmail(){
        String email = "";
        for(ActorEntity document : actorDocuments){
            email = document.getHandledBy();
            break;
        }
        return email;
    }

    @Override
    public Map<String, Integer> taskFrequency(String actorId) {
        log.info("ActorServiceImpl [taskFrequency] {}", actorId);

        Map<String, Integer> response = new HashMap<>();

        for(ActorEntity document : actorDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                int visited = task.getVisited();
                response.put(taskId, response.getOrDefault(taskId, 0) + visited);
            }
        }
        return response;
    }

    @Override
    public Map<String, Integer> retryFrequency(String actorId){
        log.info("ActorServiceImpl [retryFrequency] {}", actorId);
        Map<String, Integer> response = new HashMap<>();

        for(ActorEntity document : actorDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                int visited = task.getVisited();
                visited -= 1;
                response.put(taskId, response.getOrDefault(taskId, 0) + visited);
            }
        }

        return response;
    }

    @Override
    public Map<String, Integer>retryFrequencyThreshold(){
        log.info("ActorServiceImpl [retryFrequency] ");
        Map<String, Integer> response = new HashMap<>();

        for(ActorEntity document : allDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                int visited = task.getVisited();
                visited -= 1;
                response.put(taskId, response.getOrDefault(taskId, 0) + visited);
            }
        }

        return response;
    }

    @Override
    public Map<String, Double> taskRetries(String actorId){
        log.info("ActorServiceImpl [taskRetries]");
        Map<String, Double> response = new HashMap<>();

        Map<String, Integer> frequencyOfTasksAcrossApplications = new HashMap<>();
        Map<String, Integer> retriesForTask = retryFrequency(actorId);

        for(ActorEntity document : actorDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                frequencyOfTasksAcrossApplications.put(taskId, frequencyOfTasksAcrossApplications.getOrDefault(taskId, 0) + 1);
            }
        }

        for(Map.Entry<String, Integer> entry : retriesForTask.entrySet()){
            String taskId = entry.getKey();
            Integer retries = entry.getValue();
            Integer freq = frequencyOfTasksAcrossApplications.get(taskId);

            Double average = (double) (retries / freq);
            response.put(taskId, average);
        }

        return response;
    }

    @Override
    public Map<String, Double> taskRetriesThreshold(){
        log.info("ActorServiceImpl [taskRetriesThreshold]");
        Map<String, Double> response = new HashMap<>();

        Map<String, Integer> frequencyOfTasksAcrossApplications = new HashMap<>();
        Map<String, Integer> retriesForTask = retryFrequencyThreshold();

        for(ActorEntity document : allDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                frequencyOfTasksAcrossApplications.put(taskId, frequencyOfTasksAcrossApplications.getOrDefault(taskId, 0) + 1);
            }
        }

        for(Map.Entry<String, Integer> entry : retriesForTask.entrySet()){
            String taskId = entry.getKey();
            Integer retries = entry.getValue();
            Integer freq = frequencyOfTasksAcrossApplications.get(taskId);

            Double average = ((retries*1.0) / (freq*1.0));
            response.put(taskId, average);
        }

        return response;
    }

    @Override
    public Map<String, Integer> taskFrequencyThreshold() {
        log.info("ActorServiceImpl [taskFrequencyThreshold]");

        Map<String, Integer> response = new HashMap<>();

        for(ActorEntity document : allDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String taskId = task.getTaskId();
                int visited = task.getVisited();
                response.put(taskId, response.getOrDefault(taskId, 0) + visited);
            }
        }
        return response;
    }

    @Override
    public Map<String, Double> getTaskTimeAcrossApplications(String actorId) {
        log.info("ActorServiceImpl [getTaskTimeAcrossApplications] {}", actorId);

        Map<String, Double> taskTimeMap = new HashMap<>();

        for (ActorEntity document : actorDocuments){
            for(TaskEntity task : document.getTasks()){
                String taskId = task.getTaskId();
                double duration = task.getDuration();

                taskTimeMap.put(taskId, taskTimeMap.getOrDefault(taskId, 0.0) + duration);
            }
        }
        return taskTimeMap;
    }

    @Override
    public Map<String, Double> thresholdTaskTimeAcrossApplications() {
        log.info("ActorServiceImpl [thresholdTaskTimeAcrossApplications]");

        Map<String, Double> taskTimeMap = new HashMap<>();

        for (ActorEntity document : allDocuments){
            for(TaskEntity task : document.getTasks()){
                String taskId = task.getTaskId();
                double duration = task.getDuration();

                taskTimeMap.put(taskId, taskTimeMap.getOrDefault(taskId, 0.0) + duration);
            }
        }
        return taskTimeMap;
    }

    @Override
    public int getTasksCompleted(String actorId) {
        log.info("ActorServiceImpl [getTasksCompleted] {}", actorId);

        int tasksCompleted = 0;

        for(ActorEntity document : actorDocuments){
            List<TaskEntity> tasks = document.getTasks();
            for(TaskEntity task : tasks){
                String status = task.getStatus();
                if(status.equals("COMPLETED")){
                    tasksCompleted += 1;
                }
            }
        }
        return tasksCompleted;
    }

    @Override
    public List<Map<String, String>> getTasksAssigned(String actorId) {
        log.info("ActorServiceImpl [getTasksAssigned] {}", actorId);

        List<Map<String, String>> tasksAssigned = new ArrayList<>();

        for(ActorEntity document : actorDocuments){
            String applicationId = document.getApplicationId();

            for(TaskEntity task : document.getTasks()){
                String status = task.getStatus();
                Map<String, String> taskDetails = new HashMap<>();
                if(status.equals("NEW") || status.equals("IN_PROGRESS") || status.equals("TODO")){
                    taskDetails.put("task_name", task.getTaskId());
                    taskDetails.put("application_id", applicationId);
                    taskDetails.put("status", status);
                }
                if(!taskDetails.isEmpty()) {
                    tasksAssigned.add(taskDetails);
                }
            }
        }
        return tasksAssigned;
    }

    @Override
    public Map<String, Double> getAverageTaskTime(String actorId){
        log.info("ActorServiceImpl [getAverageTaskTime] {}", actorId);

        Map<String, Double> response = new HashMap<>();

        Map<String, Integer> taskFrequency = taskFrequency(actorId);
        Map<String, Double> taskTimeAcrossApplications = getTaskTimeAcrossApplications(actorId);

        for (Map.Entry<String, Integer> entry : taskFrequency.entrySet()) {
            String taskId = entry.getKey();
            int visited = entry.getValue();
            Double time = taskTimeAcrossApplications.get(taskId);

            Double average = time / visited;
            response.put(taskId, average);
        }

        return response;
    }

    @Override
    public Map<String, Double> thresholdAverageTaskTime(){
        log.info("ActorServiceImpl [thresholdAverageTaskTime]");

        Map<String, Double> response = new HashMap<>();

        Map<String, Integer> taskFrequency = taskFrequencyThreshold();
        Map<String, Double> taskTimeAcrossApplications = thresholdTaskTimeAcrossApplications();

        for (Map.Entry<String, Integer> entry : taskFrequency.entrySet()) {
            String taskId = entry.getKey();
            int visited = entry.getValue();
            Double time = taskTimeAcrossApplications.get(taskId);

            Double average = time / visited;
            response.put(taskId, average);
        }

        return response;
    }

    @Override
    public String getActorType(String actorId){
        String actorType = "";
        for(ActorEntity document : actorDocuments){
            actorType = document.getActorType();
        }
        return actorType;
    }

    @Override
    public Map<String, Object> getActorMetrics(String actorId, int days) {
        log.info("ActorServiceImpl [getActorMetrics] {}", actorId);

        Map<String, Object> response = new HashMap<>();

        getApplications(actorId, days);

        String actorType = getActorType(actorId);
        getAllApplications(actorType, days);

        Map<String, Double> taskTimeAcrossApplications = getTaskTimeAcrossApplications(actorId);
        Map<String, Double> averageTaskTime = getAverageTaskTime(actorId);
        int totalTasksCompleted = getTasksCompleted(actorId);
        List<Map<String, String>> tasksAssigned = getTasksAssigned(actorId);
        Map<String, Integer> thresholdTaskFrequency = taskFrequencyThreshold();
        Map<String, Double> thresholdTaskTime = thresholdTaskTimeAcrossApplications();
        Map<String, Double> thresholdAverageTaskTime = thresholdAverageTaskTime();
        Double taskEfficiencyScore = getTaskEffiencyScore(actorId);
        Map<String, Object> fastestAndSlowestTask = getFastestAndSlowestTask(actorId);
        Map<String, Object> mostAndLeastRetriedTask = getMostAndLeastRetriedTask(actorId);
        Map<String, Double> taskRetries = taskRetries(actorId);
        Map<String, Double> taskRetriesThreshold = taskRetriesThreshold();
        String handledBy = getActorEmail();

        if(taskTimeAcrossApplications == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskTimeAcrossApplications is empty");
        }
        if(tasksAssigned == null){
            log.warn("ActorServiceImpl [getActorMetrics] : tasksAssigned is empty");
        }
        if(averageTaskTime == null){
            log.warn("ActorServiceImpl [getActorMetrics] : getAverageTaskTime is empty");
        }
        if(thresholdAverageTaskTime == null){
            log.warn("ActorServiceImpl [getActorMetrics] : thresholdAverageTaskTime is empty");
        }
        if(fastestAndSlowestTask == null || fastestAndSlowestTask.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : fastestAndSlowestTask is empty for actorId {}", actorId);
        }
        if(mostAndLeastRetriedTask == null || mostAndLeastRetriedTask.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : mostAndLeastRetriedTask is empty for actorId {}", actorId);
        }
        if(taskRetries == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskRetries is empty for actorId {}", actorId);
        }
        if(taskRetriesThreshold == null){
            log.warn("ActorServiceImpl [getActorMetrics] :taskRetriesThreshold is empty");
        }

        response.put("average_task_time_across_applications", averageTaskTime);
        response.put("total_tasks_completed", totalTasksCompleted);
        response.put("tasks_assigned", tasksAssigned);
        response.put("threshold_average_task_time", thresholdAverageTaskTime);
        response.put("task_efficiency_score", taskEfficiencyScore);
        response.put("fastest_and_slowest_task", fastestAndSlowestTask);
        response.put("most_and_least_retried_task", mostAndLeastRetriedTask);
        response.put("average_retries", taskRetries);
        response.put("average_retries_threshold", taskRetriesThreshold);
        response.put("actor_type", actorType);
        response.put("handled_by", handledBy);
        return response;
    }
}
