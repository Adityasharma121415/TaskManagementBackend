package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationLogDao;
import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;
import com.cars24.taskmanagement.backend.service.ApplicationLogService;
import com.mongodb.client.model.changestream.OperationType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;



@Service
public class ApplicationLogServiceImpl implements ApplicationLogService {

    @Autowired
    private ApplicationLogDao applicationLogDao;

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public List<ApplicationLog> getLogsByApplicationId(String applicationId) {
        return applicationLogDao.getLogsByApplicationId(applicationId);
    }

    @PostConstruct
    @Override
    public void setupChangeStream() {
        ChangeStreamOptions options = ChangeStreamOptions.builder()
                .filter(Aggregation.newAggregation(
                        Aggregation.match(
                                Criteria.where("operationType").is("insert")
                        )
                ))
                .build();

        reactiveMongoTemplate.changeStream("applicationLogs", options, ApplicationLog.class)
                .doOnNext(event -> processChangeEvent(event))
                .subscribe();
    }

    private void processChangeEvent(ChangeStreamEvent<ApplicationLog> event) {
        ApplicationLog log = event.getBody();
        if (log != null && log.getApplicationId() != null) {
            String url = "http://localhost:8080/applicationLog/" + log.getApplicationId();
            webClientBuilder.build().get().uri(url).retrieve().bodyToMono(String.class).subscribe();
        }
    }
}