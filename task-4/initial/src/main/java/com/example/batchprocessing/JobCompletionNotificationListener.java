package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
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
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("=== JOB FINISHED! Проверяем результат в таблице products ===");

jdbcTemplate.query(
    "SELECT productId, productSku, productName, productAmount, productData FROM products ORDER BY productId",
    (rs, row) -> """
            ID=%d | SKU=%d | %s | Amount=%d | Data=%s
            """.formatted(
            rs.getLong("productId"),
            rs.getLong("productSku"),
            rs.getString("productName"),
            rs.getLong("productAmount"),
            rs.getString("productData")))
    .forEach(log::info);

            log.info("=== Всего обработано строк: {} ===",
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class));
            
            // Также покажем данные из таблицы лояльности
            log.info("=== Данные в таблице loyality_data ===");
            jdbcTemplate.query(
                    "SELECT productSku, loyalityData FROM loyality_data ORDER BY productSku",
                    (rs, row) -> "SKU=%d | LoyalityData=%s".formatted(
                            rs.getLong("productSku"),
                            rs.getString("loyalityData")))
                    .forEach(log::info);
        }
    }
}