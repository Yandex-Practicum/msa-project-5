package ru.kozhanov.processor;

import ru.kozhanov.model.ProductCsvRow;
import ru.kozhanov.model.ProductUpsertRow;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoyaltyEnrichmentProcessor implements ItemProcessor<ProductCsvRow, ProductUpsertRow> {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, BigDecimal> cache = new ConcurrentHashMap<>();

    public LoyaltyEnrichmentProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductUpsertRow process(ProductCsvRow item) {
        String code = (item.getLoyaltyCode() == null || item.getLoyaltyCode().isBlank())
                ? "NONE"
                : item.getLoyaltyCode().trim().toUpperCase();

        BigDecimal discount = cache.computeIfAbsent(code, c -> {
            BigDecimal d = jdbcTemplate.query(
                    "SELECT discount_percent FROM loyality_data WHERE loyalty_code = ?",
                    rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO,
                    c
            );
            return d == null ? BigDecimal.ZERO : d;
        });

        BigDecimal base = BigDecimal.valueOf(item.getBasePrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = base.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        ProductUpsertRow out = new ProductUpsertRow();
        out.setSku(item.getSku());
        out.setName(item.getName());
        out.setCategory(item.getCategory());
        out.setBasePrice(base);
        out.setLoyaltyCode(code);
        out.setDiscountPercent(discount.setScale(2, RoundingMode.HALF_UP));
        out.setFinalPrice(finalPrice);
        return out;
    }
}
