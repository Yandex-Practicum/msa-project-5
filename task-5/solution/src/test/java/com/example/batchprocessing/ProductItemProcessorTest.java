package com.example.batchprocessing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductItemProcessorTest {
    private final ProductItemProcessor processor = new ProductItemProcessor();

    @Test
    void acceptsValidProduct() {
        Product product = new Product(1L, 20001L, "hammer", 45L, "Loyality_off");
        assertEquals(product, processor.process(product));
    }

    @Test
    void rejectsNegativeAmount() {
        Product product = new Product(1L, 20001L, "hammer", -1L, "Loyality_off");
        assertThrows(IllegalArgumentException.class, () -> processor.process(product));
    }
}
