package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Date;

@Data
@Document(collection = "sendback_config")
public class SendbackConfig {

    @Id
    private String id;
    private String sourceFunnel;
    private String sourceSubModule;
    private String reason;
    private String category;
    private List<SubReason> subReasonList;
    private Date createdAt;
    private Date updatedAt;
    private String version;

    @Data
    public static class SubReason {
        private String subReason;
        private String sendbackKey;
        private String targetFunnel;
        private String targetLoanStage;
        private String targetTaskId;
        private boolean rejectionAllowed;
    }
}
