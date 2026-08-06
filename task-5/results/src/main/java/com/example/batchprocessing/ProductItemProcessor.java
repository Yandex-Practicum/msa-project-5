package com.example.batchprocessing;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

final class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    private final JdbcTemplate jdbcTemplate;
    private volatile Map<Long, String> loyaltyBySku;

    ProductItemProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    ProductItemProcessor(Map<Long, String> loyaltyBySku) {
        this.jdbcTemplate = null;
        this.loyaltyBySku = Map.copyOf(loyaltyBySku);
    }

    @Override
    public Product process(Product product) {
        String enrichedData = loyaltyBySku().getOrDefault(product.productSku(), product.productData());
        Product enriched = new Product(
                product.productId(),
                product.productSku(),
                product.productName(),
                product.productAmount(),
                enrichedData
        );
        log.debug("Enriched productSku={}", product.productSku());
        return enriched;
    }

    private Map<Long, String> loyaltyBySku() {
        Map<Long, String> current = loyaltyBySku;
        if (current == null) {
            synchronized (this) {
                current = loyaltyBySku;
                if (current == null) {
                    current = loadLoyaltyData();
                    loyaltyBySku = current;
                }
            }
        }
        return current;
    }

    private Map<Long, String> loadLoyaltyData() {
        Map<Long, String> result = jdbcTemplate.query(
                "SELECT product_sku, loyality_data FROM loyality_data",
                resultSet -> {
                    Map<Long, String> values = new HashMap<>();
                    while (resultSet.next()) {
                        values.put(resultSet.getLong("product_sku"), resultSet.getString("loyality_data"));
                    }
                    return values;
                }
        );
        log.info("Loaded loyalty reference data: entries={}", result.size());
        return Map.copyOf(result);
    }
}
