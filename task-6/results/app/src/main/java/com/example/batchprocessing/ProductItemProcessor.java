package com.example.batchprocessing;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {
    private final JdbcTemplate jdbc;

    public ProductItemProcessor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Product process(Product product) {
        String loyalty = jdbc.query(
            "SELECT loyalty_data FROM loyality_data WHERE product_sku = ?",
            result -> result.next() ? result.getString(1) : product.productData(),
            product.productSku()
        );
        return new Product(product.productId(), product.productSku(), product.productName(), product.productAmount(), loyalty);
    }
}
