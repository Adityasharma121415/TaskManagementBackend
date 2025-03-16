package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.SendbackConfigEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SendbackConfigRepository extends MongoRepository<SendbackConfigEntity,String> {
    @Query("{'subReasonList.sendbackKey': ?0}")
    Optional<SendbackConfigEntity> findBySendbackKey(String sendbackKey);
}
