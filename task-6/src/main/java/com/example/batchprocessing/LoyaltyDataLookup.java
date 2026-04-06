package com.example.batchprocessing;

import java.util.Optional;

public interface LoyaltyDataLookup {

	Optional<String> findByProductSku(Long productSku);
}
