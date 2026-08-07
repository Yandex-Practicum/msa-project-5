package com.example.batchprocessing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProductItemProcessorTest {

    private final ProductItemProcessor processor = new ProductItemProcessor(
            Map.of(20001L, "Loyality_on")
    );

    @Test
    void enrichesProductWhenLoyaltyDataExists() throws Exception {
        Product source = new Product(1L, 20001L, "hammer", 45L, "Loyality_off");

        Product result = processor.process(source);

        assertThat(result.productData()).isEqualTo("Loyality_on");
        assertThat(result.productId()).isEqualTo(source.productId());
        assertThat(result.productAmount()).isEqualTo(source.productAmount());
    }

    @Test
    void keepsOriginalDataWhenLoyaltyDataDoesNotExist() throws Exception {
        Product source = new Product(3L, 40001L, "roof_shell", 256L, "Loyality_off");

        Product result = processor.process(source);

        assertThat(result).isEqualTo(source);
    }
}
