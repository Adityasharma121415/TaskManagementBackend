package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ActorService {

    public ActorEntity getActorMetrics(String actorId);
}
