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

    public Map<String, Integer> taskFrequency(String actorId);

    Map<String, Integer> taskFrequencyThreshold();

    public Map<String, Double> getTaskTimeAcrossApplications(String actorId);

    Map<String, Double> thresholdTaskTimeAcrossApplications();

    public int getTasksCompleted(String actorId);

    public List<Map<String, String>> getTasksAssigned(String actorId);

    Map<String, Double> thresholdAverageTaskTime();

    public Map<String, Object> getActorMetrics(String actorId, int days);

    public Map<String, Double> getAverageTaskTime(String actorId);

    public Date getPastDate(int days);

    public void getAllApplications(int days);
}
