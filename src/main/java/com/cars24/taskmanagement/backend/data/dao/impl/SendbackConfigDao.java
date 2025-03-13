package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.entity.SendbackConfig;
import com.cars24.taskmanagement.backend.data.repository.SendbackConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SendbackConfigDao {

    private final SendbackConfigRepository sendbackRepo;

    public Optional<SendbackConfig> findBySendbackKey(String sendbackkey){

        return sendbackRepo.findBySendbackKey(sendbackkey);
    }

}
