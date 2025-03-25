package com.cars24.taskmanagement.backend.constants;

public class FileConstants {

    public static final String REDIS_HOST = "redis-19810.crce182.ap-south-1-1.ec2.redns.redis-cloud.com";
    public static final int REDIS_PORT = 19810;
    public static final String REDIS_PASSWORD = "5YnWMTjGEyIM3EOPemIE1A5OPEV3C0hR";
    public static final boolean REDIS_SSL = false;
    public static final int MAX_IDLE = 5;
    public static final int MIN_IDLE = 2;
    public static final int TOTAL_CONNECTIONS = 10;
    public static final String RESUME_TOKEN_COLLECTION = "resume_tokens";
    public static final String RESUME_TOKEN_KEY = "change_stream_resume_token";
    public static final int TTL = 1080;
    public static final int REDISSON_CLIENT_WAIT_TIME = 10;
    public static final int REDISSON_CLIENT_LEASE_TIME = 30;
    public static final int REDISSON_MIN_IDLE_CONNECTIONS = 4;
    public static final int REDISSON_RETRY_ATTEMPTS = 5;
}
