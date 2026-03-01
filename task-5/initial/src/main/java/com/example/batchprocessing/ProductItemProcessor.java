package com.example.batchprocessing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private Counter itemsProcessed;
    private Counter itemsEnriched;
    private Timer processingTimer;
    
    // Инициализация метрик при первом использовании
    private void initMetrics() {
        if (itemsProcessed == null) {
            itemsProcessed = Counter.builder("batch.items.processed")
                .description("Number of items processed")
                .tag("component", "processor")
                .register(meterRegistry);
        }
        
        if (itemsEnriched == null) {
            itemsEnriched = Counter.builder("batch.items.enriched")
                .description("Number of items enriched with loyality data")
                .tag("component", "processor")
                .register(meterRegistry);
        }
        
        if (processingTimer == null) {
            processingTimer = Timer.builder("batch.processing.duration")
                .description("Time taken to process items")
                .tag("component", "processor")
                .register(meterRegistry);
        }
    }

    @Override
    public Product process(final Product product) {
        initMetrics();
        
        return processingTimer.record(() -> {
            itemsProcessed.increment();
            log.debug("Processing product: SKU={}, Name={}", product.productSku(), product.productName());
            
            String loyalityData = null;

            try {
                long startTime = System.currentTimeMillis();
                
                Loyality loyality = jdbcTemplate.queryForObject(
                        "SELECT * FROM loyality_data WHERE product_sku = ?",
                        new DataClassRowMapper<>(Loyality.class),
                        product.productSku()
                );
                
                long queryTime = System.currentTimeMillis() - startTime;
                loyalityData = loyality.loyalityData();
                itemsEnriched.increment();
                
                log.info("Found loyality data for SKU: {} -> {} (query time: {} ms)", 
                    product.productSku(), loyalityData, queryTime);
                
                Timer.builder("batch.db.query.duration")
                    .description("Database query duration")
                    .tag("table", "loyality_data")
                    .register(meterRegistry)
                    .record(queryTime, TimeUnit.MILLISECONDS);
                    
            } catch (EmptyResultDataAccessException e) {
                log.warn("Loyality not found for SKU: {}, using original data", product.productSku());
                loyalityData = product.productData();
                
                meterRegistry.counter("batch.enrichment.missed",
                    "reason", "no_loyality_data").increment();
                    
            } catch (Exception e) {
                log.error("Error fetching loyality data for SKU: {}", product.productSku(), e);
                loyalityData = product.productData();
                
                meterRegistry.counter("batch.enrichment.errors",
                    "error_type", e.getClass().getSimpleName()).increment();
            }

            Product enrichedProduct = new Product(
                    product.productId(),
                    product.productSku(),
                    product.productName(),
                    product.productAmount(),
                    loyalityData
            );
            
            log.debug("Enriched product: {}", enrichedProduct);
            return enrichedProduct;
        });
    }
}