package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.service.ActorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorServiceImpl implements ActorService {

    private final ActorDaoImpl actorDao;
    @Override
    public ActorEntity getActorMetrics(String actorId) {
        log.info("ActorServiceImpl [getActorMetrics] {}", actorId);
        return actorDao.getActor(actorId);
    }
}
