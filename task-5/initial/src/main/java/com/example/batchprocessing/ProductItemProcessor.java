package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public Product process(final Product product) {
		final Long productId = product.productId();
		final Long productSku = product.productSku();
		final String productName = product.productName();
		final Long productAmount = product.productAmount();
		final String productData = product.productData();

		String loyalityData = productData;
		try {
			String sql = "SELECT loyalityData FROM loyality_data WHERE productSku = ?";
			loyalityData = jdbcTemplate.queryForObject(sql, String.class, productSku);
		} catch (EmptyResultDataAccessException ex) {
			log.debug("Нет строки лояльности для sku={}, оставляем productData", productSku);
		}

		Product out = new Product(productId, productSku, productName, productAmount, loyalityData);
		log.info("Transforming ({}) into ({})", product, out);
		return out;
	}

}
