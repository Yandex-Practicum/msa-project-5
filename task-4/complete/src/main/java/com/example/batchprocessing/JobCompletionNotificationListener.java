package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
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
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("========================================");
            log.info("=== JOB ЗАВЕРШЁН УСПЕШНО ===");
            log.info("Время старта: {}", jobExecution.getStartTime());
            log.info("Время финиша: {}", jobExecution.getEndTime());
            log.info("========================================");

            log.info("=== ДАННЫЕ В ТАБЛИЦЕ products ПОСЛЕ ОБРАБОТКИ ===");
            jdbcTemplate.query(
                    "SELECT productId, productSku, productName, productAmount, productData FROM products ORDER BY productId",
                    (rs, row) -> {
                        log.info("ID={} | SKU={} | Name={} | Amount={} | Data={}",
                                rs.getLong("productId"),
                                rs.getLong("productSku"),
                                rs.getString("productName"),
                                rs.getLong("productAmount"),
                                rs.getString("productData"));
                        return null;
                    }
            );
            log.info("========================================");
        }
    }
}