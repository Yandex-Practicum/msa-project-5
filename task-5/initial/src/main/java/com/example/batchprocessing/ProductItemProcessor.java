package com.example.batchprocessing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Timer;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private MetricsService metricsService;

    @Override
    public Product process(final Product product) {
        // Start timer for processing metrics
        Timer.Sample sample = metricsService.startProductProcessingTimer();
        
        try {
            log.info("Processing product: {}", product);
            
            // Increment products processed counter
            metricsService.incrementProductsProcessed();

            // Получаем данные лояльности для данного productSku
            String sql = "SELECT * FROM loyality_data WHERE productSku = ?";
            List<Loyality> loyalityList = jdbcTemplate.query(sql, new DataClassRowMapper<>(Loyality.class), product.productSku());

            String updatedProductData = product.productData();
            boolean loyaltyUpdated = false;

            // Если найдены данные лояльности, обновляем productData
            if (!loyalityList.isEmpty()) {
                Loyality loyality = loyalityList.get(0);
                updatedProductData = loyality.loyalityData();
                loyaltyUpdated = true;
                metricsService.incrementProductsWithLoyaltyUpdated();
                log.info("Updated loyalty data for product {}: {} -> {}", product.productSku(), product.productData(), updatedProductData);
            } else {
                metricsService.incrementProductsWithLoyaltyKept();
                log.info("No loyalty data found for product {}, keeping original data: {}", product.productSku(), product.productData());
            }

            // Создаем обновленный продукт
            Product updatedProduct = new Product(
                    product.productId(),
                    product.productSku(),
                    product.productName(),
                    product.productAmount(),
                    updatedProductData
            );

            log.info("Processed product: {} -> {}", product, updatedProduct);
            return updatedProduct;
            
        } finally {
            // Record processing time
            metricsService.recordProductProcessingTime(sample);
        }
    }

}
