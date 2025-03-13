package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.SendbackConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SendbackConfigRepository extends MongoRepository<SendbackConfig,String> {
    @Query("{'subReasonList.sendbackKey': ?0}")
    Optional<SendbackConfig> findBySendbackKey(String sendbackKey);
}
