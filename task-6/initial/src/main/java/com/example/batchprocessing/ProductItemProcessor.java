package com.example.batchprocessing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Counter loyaltyUpdatesCounter;
    private final Counter loyaltyFailedCounter;

    public ProductItemProcessor(MeterRegistry registry) {
        this.loyaltyUpdatesCounter = Counter.builder("loyalty.updates.total")
                .description("Total loyalty updates")
                .register(registry);
        this.loyaltyFailedCounter = Counter.builder("loyalty.updates.failed")
                .description("Failed loyalty updates")
                .register(registry);
    }

    @Override
    public Product process(final Product product) {
        try {
            List<Loyality> loyalityList = jdbcTemplate.query(
                "SELECT * FROM loyality_data WHERE productSku = ?",
                new DataClassRowMapper<>(Loyality.class), 
                product.productSku()
            );
            
            if (!loyalityList.isEmpty()) {
                Loyality loyality = loyalityList.get(0);
                log.info("Updated product {} with loyality data: {}", product.productId(), loyality.loyalityData());
                loyaltyUpdatesCounter.increment();
                return new Product(
                    product.productId(), 
                    product.productSku(), 
                    product.productName(), 
                    product.productAmount(), 
                    loyality.loyalityData()
                );
            } else {
                log.warn("No loyality data found for sku {}", product.productSku());
                loyaltyFailedCounter.increment();
                return product;
            }
        } catch (Exception e) {
            log.error("Error processing product with sku {}: {}", product.productSku(), e.getMessage());
            loyaltyFailedCounter.increment();
            return product;
        }
    }
}