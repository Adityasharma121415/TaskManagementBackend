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

//        response.put("average_duration", averageDuration);
        response.put("task_frequency", taskFrequency);
        response.put("task_time_across_applications", taskTimeAcrossApplications);
        response.put("average_task_time_across_applications", averageTaskTime);
        response.put("total_tasks_completed", totalTasksCompleted);
        response.put("tasks_assigned", tasksAssigned);
        response.put("threshold_average_task_time", thresholdAverageTaskTime);

        return response;
    }
}
