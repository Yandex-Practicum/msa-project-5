package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Обогащает строку отчёта данными программы лояльности:
 * если для productSku найдена запись в справочнике loyality_data,
 * поле productData заменяется актуальным значением из справочника.
 */
@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	private final JdbcTemplate jdbcTemplate;

	public ProductItemProcessor(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Product process(final Product product) {
		Optional<String> loyalityData = jdbcTemplate
				.query("SELECT productSku, loyalityData FROM loyality_data WHERE productSku = ?",
						new DataClassRowMapper<>(Loyality.class), product.productSku())
				.stream()
				.findAny()
				.map(Loyality::loyalityData);

		Product enriched = loyalityData
				.map(data -> new Product(product.productId(), product.productSku(),
						product.productName(), product.productAmount(), data))
				.orElse(product);

		log.info("Transforming ({}) into ({})", product, enriched);

		return enriched;
	}

}
