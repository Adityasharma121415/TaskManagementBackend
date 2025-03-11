package com.cars24.taskmanagement.backend.service.impl;


import com.cars24.taskmanagement.backend.data.dao.AgentDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.service.LoanDurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoanDurationServiceImpl implements LoanDurationService {

    @Autowired
    private AgentDao.LoanDurationDao loanDurationDao;

    @Override
    public Optional<LoanDuration> fetchLoanDuration(String applicationId) {
        return loanDurationDao.getLoanDurationByApplicationId(applicationId);
    }
}