package com.example.batchprocessing;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {
    @Override
    public Product process(Product product) {
        if (product.productId() == null || product.productSku() == null || product.productAmount() == null
                || product.productAmount() < 0 || product.productName() == null || product.productName().isBlank()) {
            throw new IllegalArgumentException("Некорректная строка товара: " + product);
        }
        return product;
    }
}
