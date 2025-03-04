package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

    @Data

    @NoArgsConstructor
    @AllArgsConstructor
    public class ApiResponse {
        private int statusCode;
        private boolean success;
        private String message;
        private String service;
        private Object data;
        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }


    }

