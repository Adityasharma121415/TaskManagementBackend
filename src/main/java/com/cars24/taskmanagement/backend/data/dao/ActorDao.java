package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ActorDao {

    public List<ActorEntity> getApplications(String actorId);
}
