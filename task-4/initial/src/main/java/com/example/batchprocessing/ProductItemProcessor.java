package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Product process(final Product product) {
        Loyality loyality = null;

        try {
            loyality = jdbcTemplate.queryForObject(
                    "SELECT * FROM loyality_data WHERE product_sku = ?",
                    new DataClassRowMapper<>(Loyality.class),
                    product.productSku()
            );
            log.info("Found loyality data for SKU: {} -> {}", product.productSku(), loyality.loyalityData());
        } catch (Exception e) {
            log.warn("Loyality not found for SKU: {}, using default", product.productSku());
        }

        Product enrichedProduct = new Product(
                product.productId(),      
                product.productSku(),    
                product.productName(),   
                product.productAmount(),  
                loyality != null ? loyality.loyalityData() : "NO_LOYALITY" 
        );
        
        log.debug("Enriched product: {}", enrichedProduct);
        return enrichedProduct;
    }
}