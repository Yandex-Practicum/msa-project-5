package com.example.batchprocessing;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcLoyaltyDataLookup implements LoyaltyDataLookup {

	private final JdbcTemplate jdbcTemplate;

	public JdbcLoyaltyDataLookup(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<String> findByProductSku(Long productSku) {
		return jdbcTemplate.query(
			"SELECT loyalityData FROM loyality_data WHERE productSku = ?",
			(rs, rowNum) -> rs.getString("loyalityData"),
			productSku
		).stream().findFirst();
	}
}
