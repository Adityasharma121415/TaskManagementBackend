package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ActorRepository extends MongoRepository<ActorEntity, String> {

    List<ActorEntity> findAllByActorId(String actorId);
}
