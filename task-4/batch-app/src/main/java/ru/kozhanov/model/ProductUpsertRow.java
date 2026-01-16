package ru.kozhanov.batch.model;

import java.math.BigDecimal;

public class ProductUpsertRow {
    private String sku;
    private String name;
    private String category;
    private BigDecimal basePrice;
    private String loyaltyCode;
    private BigDecimal discountPercent;
    private BigDecimal finalPrice;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getLoyaltyCode() { return loyaltyCode; }
    public void setLoyaltyCode(String loyaltyCode) { this.loyaltyCode = loyaltyCode; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
}
