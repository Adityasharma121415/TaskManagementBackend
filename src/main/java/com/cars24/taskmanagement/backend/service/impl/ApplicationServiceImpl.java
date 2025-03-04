package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.repository.ApplicationRepository;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Application> getSortedApplications(String applicationId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("entityId").is(applicationId)),
                Aggregation.sort(Sort.Direction.DESC, "metadata.updatedAt")
        );

        AggregationResults<Application> results = mongoTemplate.aggregate(aggregation, "applications", Application.class);
        return results.getMappedResults();
    }
    }

