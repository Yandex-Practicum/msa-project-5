package ru.kozhanov.batch.model;

public class ProductCsvRow {
    private String sku;
    private String name;
    private String category;
    private Double basePrice;
    private String loyaltyCode;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public String getLoyaltyCode() { return loyaltyCode; }
    public void setLoyaltyCode(String loyaltyCode) { this.loyaltyCode = loyaltyCode; }
}
