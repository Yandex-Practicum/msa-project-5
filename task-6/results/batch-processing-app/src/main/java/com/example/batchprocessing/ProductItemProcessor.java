package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public Product process(final Product product) {
		// productSku передан отдельным параметром (placeholder "?"), а не конкатенацией
		// в текст SQL — так значение уходит в БД через PreparedStatement, без риска SQL-инъекции.
		List<Loyality> loyalityRows = jdbcTemplate.query(
			"SELECT productSku, loyalityData FROM loyality_data WHERE productSku = ?",
			new DataClassRowMapper<>(Loyality.class),
			product.productSku());

		// Если для этого SKU нет записи в программе лояльности — оставляем данные
		// из исходного CSV как есть, а не затираем их пустым/дефолтным значением.
		String productData = loyalityRows.stream()
			.findFirst()
			.map(Loyality::loyalityData)
			.orElse(product.productData());

		Product transformedProduct = new Product(
			product.productId(),
			product.productSku(),
			product.productName(),
			product.productAmount(),
			productData);

		log.info("Transforming ({}) into ({})", product, transformedProduct);

		return transformedProduct;
	}

}
