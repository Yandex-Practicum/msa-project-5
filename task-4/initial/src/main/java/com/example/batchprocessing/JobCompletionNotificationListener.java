package com.example.batchprocessing;

import java.util.List;

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
            log.info("Job completed successfully! Showing results:");

            // Выводим все продукты из таблицы products
            List<Product> products = jdbcTemplate.query(
                    "SELECT productId, productSku, productName, productAmount, productData FROM products ORDER BY productId",
                    new DataClassRowMapper<>(Product.class)
            );

            log.info("Total products processed: {}", products.size());
            log.info("Products in database:");
            for (Product product : products) {
                log.info("Product ID: {}, SKU: {}, Name: {}, Amount: {}, Data: {}",
                        product.productId(), product.productSku(), product.productName(),
                        product.productAmount(), product.productData());
            }

            // Выводим статистику по обновленным данным лояльности
            long loyaltyOnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE productData = 'Loyality_on'", Long.class);
            long loyaltyOffCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE productData = 'Loyality_off'", Long.class);

            log.info("Loyalty statistics:");
            log.info("  Products with loyalty ON: {}", loyaltyOnCount);
            log.info("  Products with loyalty OFF: {}", loyaltyOffCount);
        } else {
            log.error("Job failed with status: {}", jobExecution.getStatus());
        }
    }
}
