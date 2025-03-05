package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ActorDao;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.repository.ActorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorDaoImpl implements ActorDao {

    private final ActorRepository actorRepository;

    @Override
    public ActorEntity getActor(String actorId) {
        log.info("ActorDaoImpl [getActor] {}", actorId);
        return actorRepository.findByActorId(actorId);
    }
}
