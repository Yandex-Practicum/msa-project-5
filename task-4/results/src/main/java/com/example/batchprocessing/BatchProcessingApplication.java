package com.example.batchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BatchProcessingApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BatchProcessingApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
