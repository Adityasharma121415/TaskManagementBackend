package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.exceptions.DataProcessingException;
import com.cars24.taskmanagement.backend.service.ActorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorServiceImpl implements ActorService {

    private final ActorDaoImpl actorDao;

    @Override
    public Map<String, Long> getAverageDuration(String actorId){
        log.info("ActorServiceImpl [getTotalDuration] {}", actorId);

        Map<String, Long> response = new HashMap<>();
        List<ActorEntity> documents = actorDao.getApplications(actorId);

        for(ActorEntity document : documents){
            String applicationId = document.getApplicationId();
            Long applicationDuration = document.getTotalDuration();

            int visited = 0;

            for(TaskEntity task : document.getTasks()){
                visited += (task.getVisited());
            }

            Long averageDuration = 0L;
            if(visited > 0){
                averageDuration = applicationDuration/visited;
            }
            else{
                throw new DataProcessingException("Visited count cannot be zero for applicationId: " + applicationId);
            }
            response.put(applicationId, averageDuration);
        }
        return response;
    }
}
