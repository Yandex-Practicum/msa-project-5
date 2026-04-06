package com.example.batchprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ProductItemProcessorTest {

	@Test
	void shouldReplaceProductDataWhenLoyaltyEntryExists() throws Exception {
		ProductItemProcessor processor = new ProductItemProcessor(productSku -> Optional.of("Loyality_on"));
		Product source = new Product(1L, 20001L, "hammer", 45L, "Loyality_off");

		Product result = processor.process(source);

		assertEquals("Loyality_on", result.productData());
	}

	@Test
	void shouldKeepOriginalProductDataWhenLoyaltyEntryIsMissing() throws Exception {
		ProductItemProcessor processor = new ProductItemProcessor(productSku -> Optional.empty());
		Product source = new Product(3L, 40001L, "roof_shell", 256L, "Loyality_on");

		Product result = processor.process(source);

		assertEquals(source, result);
	}
}
