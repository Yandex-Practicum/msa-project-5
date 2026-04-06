package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;

public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	private final LoyaltyDataLookup loyaltyDataLookup;

	public ProductItemProcessor(LoyaltyDataLookup loyaltyDataLookup) {
		this.loyaltyDataLookup = loyaltyDataLookup;
	}

	@Override
	public Product process(final Product product) {
		String resolvedProductData = loyaltyDataLookup
			.findByProductSku(product.productSku())
			.orElse(product.productData());

		Product transformedProduct = new Product(
			product.productId(),
			product.productSku(),
			product.productName(),
			product.productAmount(),
			resolvedProductData
		);

		log.info("Transforming ({}) into ({})", product, transformedProduct);

		return transformedProduct;
	}
}
