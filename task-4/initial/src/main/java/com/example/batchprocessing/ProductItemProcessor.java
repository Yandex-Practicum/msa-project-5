package com.example.batchprocessing;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

	@Autowired
	private NamedParameterJdbcTemplate jdbc;

    @Override
	public Product process(final Product product) {
	    String productData = null;
	    try {
		    productData = jdbc.queryForObject(
			    "SELECT * FROM loyality_data WHERE productSku=:productSku",
			    Map.of("productSku", product.productSku()),
			    new DataClassRowMapper<>(Loyality.class)
		    ).loyalityData();
	    } catch (EmptyResultDataAccessException ignored) {
		    log.info("Not found loyalty_data by productSku = {}", product.productSku());
	    }

		if (productData == null) {
			productData = product.productData();
		}

	    return new Product(
			product.productId(),
		    product.productSku(),
		    product.productName(),
		    product.productAmount(),
		    productData
	    );
	}

}
