package com.cars24.taskmanagement.backend.data.repository;


import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanDurationRepository extends MongoRepository<LoanDurationEntity, String> {
    Optional<LoanDurationEntity> findByApplicationId(String applicationId);
}