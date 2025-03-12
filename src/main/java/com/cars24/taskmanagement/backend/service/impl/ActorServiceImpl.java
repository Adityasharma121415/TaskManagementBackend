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
    public void getAllApplications(int days){
        log.info("ActorServiceImpl [getAllApplications] {}", days);
        Date pastDate = getPastDate(days);
        allDocuments = actorDao.findAllApplications(pastDate);
    }

    @Override
    public double getTaskEffiencyScore(String actorId) {
        log.info("ActorServiceImpl [getTaskEfficiencyScore] {}", actorId);

        double totalTimeSpent = 0;
        int totalTasksCompleted = getTasksCompleted(actorId);

        for(ActorEntity document : actorDocuments){
            for(TaskEntity task : document.getTasks()){
                totalTimeSpent += task.getDuration();
            }
        }

        if(totalTimeSpent == 0){
            return 0;
        }

        return (totalTasksCompleted/totalTimeSpent)*100;
    }

    @Override
    public double getAgentErrorRate(String actorId) {
        log.info("ActorServiceImpl [getAgentErrorRate] {}", actorId);

        int totalTasksHandled = 0;
        int totalRetriedTasks = 0;

        for(ActorEntity document : actorDocuments){
            if(!document.getActorId().equals(actorId)) continue;

            for(TaskEntity task : document.getTasks()){
                totalTasksHandled++;
                if(task.getVisited() > 1){
                    totalRetriedTasks++;
                }
            }
        }

        if(totalTasksHandled == 0){
            return 0;
        }

        return (totalRetriedTasks / (double)totalTasksHandled)*100;
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
            result.put("most_retried_task", Map.of("task_id", mostRetriedTask.getTaskId(), "visited", mostRetriedTask.getVisited()));
        }
        if(leastRetriedTask != null){
            result.put("least_retried_task", Map.of("task_id", leastRetriedTask.getTaskId(), "visited", leastRetriedTask.getVisited()));
        }
        return result;
    }

    @Override
    public Date getPastDate(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }

//    @Override
//    public Map<String, Long> getAverageDuration(String actorId){
//        log.info("ActorServiceImpl [getTotalDuration] {}", actorId);
//
//        Map<String, Long> response = new HashMap<>();
//        List<ActorEntity> documents = getApplications(actorId);
//
//        for(ActorEntity document : actorDocuments){
//            String applicationId = document.getApplicationId();
//            Long applicationDuration = document.getTotalDuration();
//
//            int visited = 0;
//
//            for(TaskEntity task : document.getTasks()){
//                visited += (task.getVisited());
//            }
//
//            Long averageDuration = 0L;
//            if(visited > 0){
//                averageDuration = applicationDuration/visited;
//            }
//            else{
//                throw new DataProcessingException("Visited count cannot be zero for applicationId: " + applicationId);
//            }
//            response.put(applicationId, averageDuration);
//        }
//        return response;
//    }

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
            tasksCompleted += document.getTasks().size();
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
                Map<String, String> taskDetails = new HashMap<>();
                taskDetails.put("task_name", task.getTaskId());
                taskDetails.put("application_id", applicationId);

                tasksAssigned.add(taskDetails);
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
    public Map<String, Object> getActorMetrics(String actorId, int days) {
        log.info("ActorServiceImpl [getActorMetrics] {}", actorId);

        Map<String, Object> response = new HashMap<>();

        getApplications(actorId, days);

        getAllApplications(days);

//        Map<String, Long> averageDuration = getAverageDuration(actorId);
        Map<String, Integer> taskFrequency = taskFrequency(actorId);
        Map<String, Double> taskTimeAcrossApplications = getTaskTimeAcrossApplications(actorId);
        Map<String, Double> averageTaskTime = getAverageTaskTime(actorId);
        int totalTasksCompleted = getTasksCompleted(actorId);
        List<Map<String, String>> tasksAssigned = getTasksAssigned(actorId);
        Map<String, Integer> thresholdTaskFrequency = taskFrequencyThreshold();
        Map<String, Double> thresholdTaskTime = thresholdTaskTimeAcrossApplications();
        Map<String, Double> thresholdAverageTaskTime = thresholdAverageTaskTime();
        Double taskEfficiencyScore = getTaskEffiencyScore(actorId);
        Double agentErrorRate = getAgentErrorRate(actorId);
        Map<String, Object> fastestAndSlowestTask = getFastestAndSlowestTask(actorId);
        Map<String, Object> mostAndLeastRetriedTask = getMostAndLeastRetriedTask(actorId);

//        if(averageDuration == null){
//            log.warn("ActorServiceImpl [getActorMetrics] : averageDuration is empty");
//        }
        if(taskFrequency == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskFrequency is empty");
        }
        if(taskTimeAcrossApplications == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskTimeAcrossApplications is empty");
        }
        if(totalTasksCompleted == 0){
            log.warn("ActorServiceImpl [getActorMetrics] : totalTasksCompleted is empty");
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
        if(taskEfficiencyScore == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskEfficiencyScore is empty for actorId {}", actorId);
        }
        if(agentErrorRate == null){
            log.warn("ActorServiceImpl [getActorMetrics] : agentErrorRate is empty for actorId {}", actorId);
        }
        if(fastestAndSlowestTask == null || fastestAndSlowestTask.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : fastestAndSlowestTask is empty for actorId {}", actorId);
        }
        if(mostAndLeastRetriedTask == null || mostAndLeastRetriedTask.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : mostAndLeastRetriedTask is empty for actorId {}", actorId);
        }

//        response.put("average_duration", averageDuration);
        response.put("task_frequency", taskFrequency);
        response.put("task_time_across_applications", taskTimeAcrossApplications);
        response.put("average_task_time_across_applications", averageTaskTime);
        response.put("total_tasks_completed", totalTasksCompleted);
        response.put("tasks_assigned", tasksAssigned);
        response.put("threshold_average_task_time", thresholdAverageTaskTime);
        response.put("task_efficiency_score", taskEfficiencyScore);
        response.put("agent_error_rate", agentErrorRate);
        response.put("fastest_and_slowest_task", fastestAndSlowestTask);
        response.put("most_and_least_retried_task", mostAndLeastRetriedTask);

        return response;
    }
}
