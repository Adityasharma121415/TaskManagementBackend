package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ActorDao;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.repository.ActorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorDaoImpl implements ActorDao {

    private final ActorRepository actorRepository;

    @Override
    public List<ActorEntity> findAllByActorIdAndLastUpdatedAtAfter(String actorId, Date filteredDate) {
        log.info("ActorDaoImpl [getDuration] {} {}", actorId, filteredDate);
        return actorRepository.findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate);
    }

    @Override
    public List<ActorEntity> findAllApplications(String actorType, Date pastDate) {
        log.info("ActorDaoImpl [findAllApplications] {} {}", actorType, pastDate);
        return actorRepository.findAllByLastUpdatedAtAfter(actorType, pastDate);
    }

    @Override
    public List<ActorEntity> findAllByFunnelAndLastUpdatedAtAfter(String funnel, Date filteredDate) {
        log.info("ActorDaoImpl [getDuration] {} {}", funnel, filteredDate);
        return actorRepository.findAllByFunnelAndLastUpdatedAtAfter(funnel, filteredDate);
    }
}
