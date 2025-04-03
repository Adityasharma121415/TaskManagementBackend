package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public interface ActorService {

    public void getApplications(String actorId, int days);

//    public Map<String, Long> getAverageDuration(String actorId, int days);

    String getActorEmail();

    public Map<String, Integer> taskFrequency(String actorId);

    Map<String, Integer> retryFrequency(String actorId);

    Map<String, Integer>retryFrequencyThreshold();

    Map<String, Double> taskRetries(String actorId);

    Map<String, Double> taskRetriesThreshold();

    Map<String, Integer> taskFrequencyThreshold();

    public Map<String, Double> getTaskTimeAcrossApplications(String actorId);

    Map<String, Double> thresholdTaskTimeAcrossApplications();

    public int getTasksCompleted(String actorId);

    public List<Map<String, String>> getTasksAssigned(String actorId);

    Map<String, Double> thresholdAverageTaskTime();

    String getActorType(String actorId);

    public Map<String, Object> getActorMetrics(String actorId, int days);

    public Map<String, Double> getAverageTaskTime(String actorId);

    public Date getPastDate(int days);

    void getAllApplications(String actorType, int days);

    public double getTaskEffiencyScore(String actorId) throws Exception;

    Map<String, Double[]> getTaskDuration(String actorId);

    Map<String, Double[]> getSystemTaskDuration(String funnel);

    public Map<String, Object> getTasksSortedByRetries(String id);

    void getSystemApplications(String funnel, int days);

    Map<String, Object> getSystemMetrics(String funnel, int days);
}
