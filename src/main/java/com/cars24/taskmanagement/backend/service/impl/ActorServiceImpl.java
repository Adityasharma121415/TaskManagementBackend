package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.service.ActorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorServiceImpl implements ActorService {

    private final ActorDaoImpl actorDao;

    private List<ActorEntity> actorDocuments = new ArrayList<>();

    private List<ActorEntity> allDocuments = new ArrayList<>();

    private List<ActorEntity> systemDocuments = new ArrayList<>();

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

        Map<String, List<Double>> actorTaskTimes = collectTaskTimes(actorDocuments, actorId);
        Map<String, List<Double>> globalTaskTimes = collectTaskTimes(allDocuments, null);

        Map<String, Map<String,Double>> agentP90 = computePercentile(actorTaskTimes);
        Map<String, Map<String,Double>> globalPercentiles = computePercentile(globalTaskTimes);

        Map<String, Double[]> globalAvgPercentiles = computeGlobalAvgPercentiles(globalPercentiles);

        return computeEfficiencyScore(agentP90, globalAvgPercentiles);
    }

    private Map<String, List<Double>> collectTaskTimes(List<ActorEntity> documents , String actorId){
        Map<String, List<Double>> taskTimes = new HashMap<>();
        for(ActorEntity document: documents){
            if(actorId!=null && !document.getActorId().equals(actorId)) continue;

            for(TaskEntity task : document.getTasks()){
                double duration = task.getDuration();
//                if(duration > 0){
                String taskId = task.getTaskId();
                taskTimes.computeIfAbsent(taskId, k->new ArrayList<>()).add(duration);
//                }
            }
        }
        return taskTimes;
    }

    private Map<String, Map<String,Double>> computePercentile(Map<String, List<Double>> taskTimes){
        Map<String, Map<String, Double>> percentiles = new HashMap<>();

        for(String taskId : taskTimes.keySet()){
            List<Double> times = taskTimes.get(taskId);
            Collections.sort(times);
//            percentiles.put(taskId, percentileCalculation(times, percentile));

            Map<String, Double> taskPercentiles = new HashMap<>();
            taskPercentiles.put("P90", percentileCalculation(times, 90));
            taskPercentiles.put("P95", percentileCalculation(times, 95));
            taskPercentiles.put("P99", percentileCalculation(times, 99));

            percentiles.put(taskId, taskPercentiles);
        }
        return percentiles;
    }

    private Map<String, Double[]> computeGlobalAvgPercentiles(Map<String, Map<String,Double>> globalPercentiles){
        Map<String, Double[]> globalAvgPercentiles = new HashMap<>();

        for(String taskId : globalPercentiles.keySet()){
            List<Double> p90List = new ArrayList<>();
            List<Double> p95List = new ArrayList<>();
            List<Double> p99List = new ArrayList<>();

            for(Map.Entry<String, Map<String, Double>> entry : globalPercentiles.entrySet()){
                Map<String, Double> percentiles = entry.getValue();

                if(percentiles.containsKey(taskId)){
                    p90List.add(percentiles.get("P90"));
                    p95List.add(percentiles.get("P95"));
                    p99List.add(percentiles.get("P99"));
                }
            }

            globalAvgPercentiles.put(taskId, new Double[]{
                    p90List.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    p95List.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    p99List.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
            });
        }
        return globalAvgPercentiles;
    }

    private double percentileCalculation(List<Double> sortedTimes, int percentile){
        if (sortedTimes.isEmpty()) return 0.0;

        int index = (int) Math.ceil((percentile / 100.0) * sortedTimes.size()) - 1;
        return sortedTimes.get(index);
    }

    private double computeEfficiencyScore(Map<String, Map<String,Double>> agentP90, Map<String, Double[]> globalAvgPercentiles){
        double totalScore = 0.0;
        int taskCount = 0;

        for(String taskId : agentP90.keySet()){
            double taskScore = computeTaskScore(taskId, agentP90, globalAvgPercentiles);

            if(taskScore != -1){
                totalScore += taskScore;
                taskCount++;
            }
        }

        return taskCount == 0 ? 0.0 : (totalScore/taskCount)*100;
    }

    //for one task id
    private double computeTaskScore(String taskId, Map<String, Map<String, Double>> agentPercentiles, Map<String, Double[]> globalAvgPercentiles){
        Map<String, Double> agentPerc = agentPercentiles.get(taskId);
        Double[] globalPerc = globalAvgPercentiles.get(taskId);

        if (globalPerc == null) return -1;

        double agentP90 = agentPerc.get("P90");
        double globalP90 = globalPerc[0];
        double globalP95 = globalPerc[1];
        double globalP99 = globalPerc[2];

        return getScore(agentP90, globalP90, globalP95, globalP99);
    }

    private double getScore(double agentP90, double globalP90, double globalP95, double globalP99){
        if(agentP90 <= globalP90) return 1;
        if(agentP90 <= globalP95) return 0.75;
        if(agentP90 <= globalP99) return 0.5;
        return 0.25;
    }

    @Override
    public Map<String, Object> getFastestAndSlowestTask(String id) {
        log.info("ActorServiceImpl [getFastestAndSlowestTask] {}", id);

        TaskEntity fastestTask = null;
        TaskEntity slowestTask = null;

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                if(!document.getActorId().equals(id)) continue;

                for(TaskEntity task : document.getTasks()){
                    if(fastestTask == null || task.getDuration() < fastestTask.getDuration()){
                        fastestTask = task;
                    }
                    if(slowestTask == null || task.getDuration() > slowestTask.getDuration()){
                        slowestTask = task;
                    }
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                if(!document.getFunnel().equals(id)) continue;

                for(TaskEntity task : document.getTasks()){
                    if(fastestTask == null || task.getDuration() < fastestTask.getDuration()){
                        fastestTask = task;
                    }
                    if(slowestTask == null || task.getDuration() > slowestTask.getDuration()){
                        slowestTask = task;
                    }
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
    public Map<String, Double[]> getTaskDuration(String actorId){
        log.info("ActorServiceImpl [getTaskDuration] {}", actorId);

        Map<String, Double[]> response = new HashMap<>();

        Map<String, List<Double>> tasks = collectTaskTimes(actorDocuments, actorId);

        for(Map.Entry<String, List<Double>> entry : tasks.entrySet()){
            Collections.sort(entry.getValue());
            Double slowestTaskP90 = percentileCalculation(entry.getValue(), 90);
            Double slowestTaskP95 = percentileCalculation(entry.getValue(), 95);
            Double slowestTaskP99 = percentileCalculation(entry.getValue(), 99);
            Double fastestTask = entry.getValue().get(0);
            response.put(entry.getKey(), new Double[]{slowestTaskP90, slowestTaskP95, slowestTaskP99, fastestTask});
        }

        return response;
    }

    @Override
    public Map<String, Double[]> getSystemTaskDuration(String funnel){
        log.info("ActorServiceImpl [getSystemTaskDuration] {}", funnel);

        Map<String, Double[]> response = new HashMap<>();

        Map<String, List<Double>> tasks = collectTaskTimes(systemDocuments, null);

        for(Map.Entry<String, List<Double>> entry : tasks.entrySet()){
            Collections.sort(entry.getValue());
            Double slowestTaskP90 = percentileCalculation(entry.getValue(), 90);
            Double slowestTaskP95 = percentileCalculation(entry.getValue(), 95);
            Double slowestTaskP99 = percentileCalculation(entry.getValue(), 99);
            Double fastestTask = entry.getValue().get(0);
            response.put(entry.getKey(), new Double[]{slowestTaskP90, slowestTaskP95, slowestTaskP99, fastestTask});
        }

        return response;
    }

    @Override
    public List<Map<String, Object>> getTasksSortedByRetries(String id){
        log.info("ActorServiceImpl [getTasksSortedByRetries] {}", id);

        List<ActorEntity> sourceDocuments = id.matches("\\d+") ? actorDocuments : systemDocuments;

        Map<String, Integer> taskVisitCounts = new HashMap<>();

        for(ActorEntity document : sourceDocuments){
            if (id.matches("\\d+") && !document.getActorId().equals(id)) continue;
            if (!id.matches("\\d+") && !document.getFunnel().equals(id)) continue;

            for(TaskEntity task : document.getTasks()){
                taskVisitCounts.put(task.getTaskId(),
                        taskVisitCounts.getOrDefault(task.getTaskId(), 0) + task.getVisited());
            }
        }

        return taskVisitCounts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))  // Sort in descending order
                .map(entry -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("task_id", entry.getKey());
                    taskMap.put("visited", entry.getValue() > 0 ? entry.getValue() - 1 : 0);
                    return taskMap;
                })
                .collect(Collectors.toList());
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
    public Map<String, Integer> taskFrequency(String id) {
        log.info("ActorServiceImpl [taskFrequency] {}", id);

        Map<String, Integer> response = new HashMap<>();

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    int visited = task.getVisited();
                    response.put(taskId, response.getOrDefault(taskId, 0) + visited);
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    int visited = task.getVisited();
                    response.put(taskId, response.getOrDefault(taskId, 0) + visited);
                }
            }
        }

        return response;
    }

    @Override
    public Map<String, Integer> retryFrequency(String id){
        log.info("ActorServiceImpl [retryFrequency] {}", id);
        Map<String, Integer> response = new HashMap<>();

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    int visited = task.getVisited();
                    visited -= 1;
                    response.put(taskId, response.getOrDefault(taskId, 0) + visited);
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    int visited = task.getVisited();
                    visited -= 1;
                    response.put(taskId, response.getOrDefault(taskId, 0) + visited);
                }
            }
        }

        return response;
    }

    @Override
    public Map<String, Integer>retryFrequencyThreshold(){
        log.info("ActorServiceImpl [retryFrequencyThreshold] ");
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
    public Map<String, Double> taskRetries(String id){
        log.info("ActorServiceImpl [taskRetries] {}", id);
        Map<String, Double> response = new HashMap<>();

        Map<String, Integer> frequencyOfTasksAcrossApplications = new HashMap<>();
        Map<String, Integer> retriesForTask = retryFrequency(id);

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    frequencyOfTasksAcrossApplications.put(taskId, frequencyOfTasksAcrossApplications.getOrDefault(taskId, 0) + 1);
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String taskId = task.getTaskId();
                    frequencyOfTasksAcrossApplications.put(taskId, frequencyOfTasksAcrossApplications.getOrDefault(taskId, 0) + 1);
                }
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
    public Map<String, Double> getTaskTimeAcrossApplications(String id) {
        log.info("ActorServiceImpl [getTaskTimeAcrossApplications] {}", id);

        Map<String, Double> taskTimeMap = new HashMap<>();

        if(id.matches("\\d+")){
            for (ActorEntity document : actorDocuments){
                for(TaskEntity task : document.getTasks()){
                    String taskId = task.getTaskId();
                    double duration = task.getDuration();
                    taskTimeMap.put(taskId, taskTimeMap.getOrDefault(taskId, 0.0) + duration);
                }
            }
        }
        else{
            for (ActorEntity document : systemDocuments){
                for(TaskEntity task : document.getTasks()){
                    String taskId = task.getTaskId();
                    double duration = task.getDuration();
                    taskTimeMap.put(taskId, taskTimeMap.getOrDefault(taskId, 0.0) + duration);
                }
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
    public int getTasksCompleted(String id) {
        log.info("ActorServiceImpl [getTasksCompleted] {}", id);

        int tasksCompleted = 0;

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String status = task.getStatus();
                    if(status.equals("COMPLETED")){
                        tasksCompleted += 1;
                    }
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                List<TaskEntity> tasks = document.getTasks();
                for(TaskEntity task : tasks){
                    String status = task.getStatus();
                    if(status.equals("COMPLETED")){
                        tasksCompleted += 1;
                    }
                }
            }
        }

        return tasksCompleted;
    }

    @Override
    public List<Map<String, String>> getTasksAssigned(String id) {
        log.info("ActorServiceImpl [getTasksAssigned] {}", id);

        List<Map<String, String>> tasksAssigned = new ArrayList<>();

        if(id.matches("\\d+")){
            for(ActorEntity document : actorDocuments){
                String applicationId = document.getApplicationId();
                for(TaskEntity task : document.getTasks()){
                    String status = task.getStatus();
                    Map<String, String> taskDetails = new HashMap<>();
                    if(status.equals("NEW") || status.equals("IN_PROGRESS") || status.equals("TODO") || status.equals("FAILED")){
                        taskDetails.put("task_name", task.getTaskId());
                        taskDetails.put("application_id", applicationId);
                        taskDetails.put("status", status);
                    }
                    if(!taskDetails.isEmpty()) {
                        tasksAssigned.add(taskDetails);
                    }
                }
            }
        }
        else{
            for(ActorEntity document : systemDocuments){
                String applicationId = document.getApplicationId();
                for(TaskEntity task : document.getTasks()){
                    String status = task.getStatus();
                    Map<String, String> taskDetails = new HashMap<>();
                    if(status.equals("NEW") || status.equals("IN_PROGRESS") || status.equals("TODO") || status.equals("FAILED")){
                        taskDetails.put("task_name", task.getTaskId());
                        taskDetails.put("application_id", applicationId);
                        taskDetails.put("status", status);
                    }
                    if(!taskDetails.isEmpty()) {
                        tasksAssigned.add(taskDetails);
                    }
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
        log.info("ActorServiceImpl [getActorMetrics] {} {}", actorId, days);

        Map<String, Object> response = new HashMap<>();

        if (actorId == null || !actorId.matches("\\d+") || Integer.parseInt(actorId) <= 0) {
            response.put("Error","Actor ID should be a number greater than 0.");
            return response;
        }

        if(days < 7 || days > 90){
            response.put("Error", "Days must be greater than 7 and less than 90.");
            return response;
        }

        getApplications(actorId, days);

        if(actorDocuments.isEmpty()){
            response.put("Error","Actor data unavailable.");
            return response;
        }

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
//        Map<String, Object> mostAndLeastRetriedTask = getMostAndLeastRetriedTask(actorId);
        List<Map<String, Object>> tasksSortedByRetries = getTasksSortedByRetries(actorId);
        Map<String, Double> taskRetries = taskRetries(actorId);
        Map<String, Double> taskRetriesThreshold = taskRetriesThreshold();
        String handledBy = getActorEmail();
        Map<String, Double[]> taskDuration = getTaskDuration(actorId);

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
//        if(mostAndLeastRetriedTask == null || mostAndLeastRetriedTask.isEmpty()){
//            log.warn("ActorServiceImpl [getActorMetrics] : mostAndLeastRetriedTask is empty for actorId {}", actorId);
//        }
        if(taskDuration == null){
            log.warn("ActorServiceImpl [getActorMetrics] : getTaskDuration is empty");
        }
        if(tasksSortedByRetries == null){
            log.warn("ActorServiceImpl [getActorMetrics] : tasksSortedByRetries is empty for actorId {}", actorId);
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
//        response.put("most_and_least_retried_task", mostAndLeastRetriedTask);
        response.put("tasks_sorted_by_retries", tasksSortedByRetries);
        response.put("average_retries", taskRetries);
        response.put("average_retries_threshold", taskRetriesThreshold);
        response.put("actor_type", actorType);
        response.put("handled_by", handledBy);
        response.put("task_duration", taskDuration);
        return response;
    }

    @Override
    public void getSystemApplications(String funnel, int days){
        log.info("ActorServiceImpl [getSystemApplications] {} {}", funnel, days);
        Date pastDate = getPastDate(days);
        systemDocuments = actorDao.findAllByFunnelAndLastUpdatedAtAfter(funnel, pastDate);
    }

    @Override
    public Map<String, Object> getSystemMetrics(String funnel, int days) {
        log.info("ActorServiceImpl [getSystemMetrics] {} {}", funnel, days);

        Map<String, Object> response = new HashMap<>();

        if (funnel == null) {
            response.put("Error","Funnel should be a word.");
            return response;
        }

        if(days < 7 || days > 90){
            response.put("Error", "Days must be greater than 7 and less than 90.");
            return response;
        }

        getSystemApplications(funnel, days);
        log.info("systemDocuments {}", systemDocuments);

        if(systemDocuments.isEmpty()){
            response.put("Error","Funnel data unavailable.");
            return response;
        }

        int totalTasksCompleted = getTasksCompleted(funnel);
        Map<String, Object> fastestAndSlowestTask = getFastestAndSlowestTask(funnel);
        List<Map<String, Object>> tasksSortedByRetries = getTasksSortedByRetries(funnel);
        Map<String, Double> averageTaskTime = getAverageTaskTime(funnel);
        Map<String, Double> taskRetries = taskRetries(funnel);
        List<Map<String, String>> tasksAssigned = getTasksAssigned(funnel);
        Map<String, Double[]> systemTaskDuration = getSystemTaskDuration(funnel);

        if(tasksAssigned == null){
            log.warn("ActorServiceImpl [getActorMetrics] : tasksAssigned is empty");
        }
        if(averageTaskTime == null){
            log.warn("ActorServiceImpl [getActorMetrics] : getAverageTaskTime is empty");
        }
        if(fastestAndSlowestTask == null || fastestAndSlowestTask.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : fastestAndSlowestTask is empty for actorId {}", funnel);
        }
        if(tasksSortedByRetries == null || tasksSortedByRetries.isEmpty()){
            log.warn("ActorServiceImpl [getActorMetrics] : getTasksSortedByRetries is empty for actorId {}", funnel);
        }
        if(taskRetries == null){
            log.warn("ActorServiceImpl [getActorMetrics] : taskRetries is empty for actorId {}", funnel);
        }

        response.put("task_duration", systemTaskDuration);
        response.put("average_task_time_across_applications", averageTaskTime);
        response.put("total_tasks_completed", totalTasksCompleted);
        response.put("tasks_assigned", tasksAssigned);
        response.put("fastest_and_slowest_task", fastestAndSlowestTask);
//        response.put("most_and_least_retried_task", mostAndLeastRetriedTask);
        response.put("tasks_sorted_by_retries", tasksSortedByRetries);
        response.put("average_retries", taskRetries);
        return response;
    }
}
