package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Product process(final Product product) throws Exception {
        final Long productId = product.productId();
        final Long productSku = product.productSku();
        final String productName = product.productName();
        final Long productAmount = product.productAmount();
        final String productData = product.productData();

        // Получаем опциональные данные о лояльности
        Optional<Loyality> loyalityOpt = jdbcTemplate.query(
                "SELECT * FROM loyality_data WHERE productSku = ?",
                new DataClassRowMapper<>(Loyality.class),
                productSku
        ).stream().findAny();

        // Если есть данные о лояльности — обновляем productData
        Product transformedProduct = loyalityOpt
                .map(loyality -> new Product(productId, productSku, productName, productAmount, loyality.loyalityData()))
                .orElse(product);

        // Логируем процесс трансформации
        log.info("Processing product: {} -> {}", product, transformedProduct);
        return transformedProduct;
    }
}
