package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "login")
public class UserEntity {

    @Id
    private String id;

    @Field("name")
    private String name;
    @Field("email")
    private String email;

    @Field("password")
    private String password;
}