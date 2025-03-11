package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.entity.LoanDuration;

import java.util.Optional;

public interface LoanDurationService {
    Optional<LoanDuration> fetchLoanDuration(String applicationId);
}