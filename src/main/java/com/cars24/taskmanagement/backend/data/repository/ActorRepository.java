package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorRepository extends MongoRepository<ActorEntity, String> {

    public ActorEntity findByActorId(String actorId);
}
