package com.example.batchprocessing;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    private final JdbcTemplate jdbcTemplate;

    public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Job finished");
        List<Product> products = jdbcTemplate.query(
            "SELECT productId, productSku, productName, productAmount, productData FROM products",
            new DataClassRowMapper<>(Product.class)
        );

        String data = products
            .stream()
            .map(Product::toString)
            .collect(Collectors.joining("\n"));

        log.info("Updated table: {}", data);
    }
}
