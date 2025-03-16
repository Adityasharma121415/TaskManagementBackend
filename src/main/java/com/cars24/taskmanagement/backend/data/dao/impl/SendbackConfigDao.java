package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.entity.SendbackConfigEntity;
import com.cars24.taskmanagement.backend.data.repository.SendbackConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SendbackConfigDao {

    private final SendbackConfigRepository sendbackRepo;

    public Optional<SendbackConfigEntity> findBySendbackKey(String sendbackkey){

        return sendbackRepo.findBySendbackKey(sendbackkey);
    }

}
