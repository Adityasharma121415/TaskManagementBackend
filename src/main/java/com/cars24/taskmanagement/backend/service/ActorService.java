package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ActorService {

    public List<ActorEntity> getApplications(String actorId);

    public Map<String, Long> getAverageDuration(String actorId);

    public Map<String, Integer> taskFrequency(String actorId);

    public Map<String, Double> getTaskTimeAcrossApplications(String actorId);

    public int getTasksCompleted(String actorId);

    public List<Map<String, String>> getTasksAssigned(String actorId);

    public Map<String, Object> getActorMetrics(String actorId);
}
