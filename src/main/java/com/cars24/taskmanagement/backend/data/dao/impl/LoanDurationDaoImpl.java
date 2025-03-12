package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.LoanDurationDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.data.repository.LoanDurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LoanDurationDaoImpl implements LoanDurationDao {


    @Autowired
    private LoanDurationRepository repository;

    @Override
    public Optional<LoanDuration> getLoanDurationByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId);
    }
}
