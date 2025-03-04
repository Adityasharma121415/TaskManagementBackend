package com.cars24.taskmanagement.backend.data.repository;

public interface ApplicationRepository extends MongoRepository<log, String> {
    List<log> findByApplicationIdOrderByUpdatedAtDesc(String applicationId);
}
