package com.example.batchclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BatchClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchClientApplication.class, args);
    }
}
