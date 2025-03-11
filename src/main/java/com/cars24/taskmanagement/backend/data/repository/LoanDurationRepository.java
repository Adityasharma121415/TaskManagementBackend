package com.cars24.taskmanagement.backend.data.repository;


import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LoanDurationRepository extends MongoRepository<LoanDuration, String> {
    Optional<LoanDuration> findByApplicationId(String applicationId);
}