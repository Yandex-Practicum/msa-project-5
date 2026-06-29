package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product process(final Product product) {
        log.info("Processing: id={}, sku={}, name={}, data={}",
                product.productId(), product.productSku(),
                product.productName(), product.productData());

        String newProductData = product.productData();

        if ("Loyality_off".equalsIgnoreCase(product.productData())) {
            String sql = "SELECT loyalityData FROM loyality_data WHERE productSku = ?";
            var results = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getString("loyalityData"),
                    product.productSku());

            if (!results.isEmpty()) {
                log.info("  -> SKU {} found. Updating to Loyality_on", product.productSku());
                newProductData = "Loyality_on";
            } else {
                log.info("  -> SKU {} not in loyality_data", product.productSku());
            }
        }

        return new Product(
                product.productId(),
                product.productSku(),
                product.productName(),
                product.productAmount(),
                newProductData
        );
    }
}