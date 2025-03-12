package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.LoanDuration;

import java.util.Optional;

public interface LoanDurationDao {

        Optional<LoanDuration> getLoanDurationByApplicationId(String applicationId);

}
