package com.cars24.taskmanagement.backend.data.dao.impl;


import com.cars24.taskmanagement.backend.data.dao.SlaDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaDaoImpl  implements SlaDao {

    private final TaskExecutionTimeRepository repository;

    public List<TaskExecutionTimeEntity> getTasksByChannel(String channel) {
        return repository.findByChannel(channel);
    }
}
