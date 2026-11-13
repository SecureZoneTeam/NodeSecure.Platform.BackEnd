package com.safezone.nodesecure.platform.inventory.domain.model.aggregates;

import com.safezone.nodesecure.platform.inventory.domain.exceptions.InsufficientStockException;
import com.safezone.nodesecure.platform.inventory.domain.model.events.ProductCreatedEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;

public final class Product extends AbstractDomainAggregateRoot {
    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int UNIT_MAX_LENGTH = 20;
    private static final int CREATED_BY_MAX_LENGTH = 100;
    private static final String DEFAULT_UNIT = "UNIT";

    private final Long id;
    private final Long companyId;
    private String name;
    private Sku sku;
    private ProductCategory category;
    private String unit;
    private int currentStock;
    private final Instant createdAt;
    private Instant updatedAt;
    private final String createdBy;

    private Product(Long id, Long companyId, String name, Sku sku, ProductCategory category, String unit,
                    int currentStock, Instant createdAt, Instant updatedAt, String createdBy) {
        this.id = id;
        this.companyId = requireCompanyId(companyId);
        this.name = normalizeName(name);
        this.sku = Objects.requireNonNull(sku);
        this.category = Objects.requireNonNull(category);
        this.unit = normalizeUnit(unit);
        if (currentStock < 0) throw new IllegalArgumentException("stock.error.countedStock.invalid");
        this.currentStock = currentStock;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.createdBy = normalizeCreatedBy(createdBy);
    }

    public static Product create(Long companyId, String name, Sku sku, ProductCategory category,
                                 String unit, String createdBy, Instant now) {
        var product = new Product(null, companyId, name, sku, category, unit, 0, now, now, createdBy);
        product.registerEvent(new ProductCreatedEvent(sku.value(), now));
        return product;
    }

    public static Product rehydrate(Long id, Long companyId, String name, Sku sku, ProductCategory category,
                                    String unit, int currentStock, Instant createdAt, Instant updatedAt,
                                    String createdBy) {
        if (id == null || id <= 0) throw new IllegalArgumentException("product.error.id.invalid");
        return new Product(id, companyId, name, sku, category, unit, currentStock, createdAt, updatedAt, createdBy);
    }

    public void updateDetails(String name, Sku sku, ProductCategory category, String unit, Instant now) {
        this.name = normalizeName(name);
        this.sku = Objects.requireNonNull(sku);
        this.category = Objects.requireNonNull(category);
        this.unit = normalizeUnit(unit);
        this.updatedAt = Objects.requireNonNull(now);
    }
    public void receive(int quantity, Instant now) {
        requirePositive(quantity);
        try {
            this.currentStock = Math.addExact(this.currentStock, quantity);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("stock.error.overflow");
        }
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void dispatch(int quantity, Instant now) {
        requirePositive(quantity);
        if (quantity > currentStock) {
            throw new InsufficientStockException(quantity, currentStock);
        }
        this.currentStock -= quantity;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void reconcileTo(int countedStock, Instant now) {
        if (countedStock < 0) throw new IllegalArgumentException("stock.error.countedStock.invalid");
        this.currentStock = countedStock;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public boolean canDispatch(int quantity) {
        return quantity > 0 && quantity <= currentStock;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("stock.error.quantity.invalid");
    }

    private static Long requireCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) throw new IllegalArgumentException("product.error.companyId.invalid");
        return companyId;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("product.error.name.notBlank");
        var trimmed = name.trim();
        if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("product.error.name.size");
        }
        return trimmed;
    }

    private static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) return DEFAULT_UNIT;
        var trimmed = unit.trim();
        if (trimmed.length() > UNIT_MAX_LENGTH) throw new IllegalArgumentException("product.error.unit.size");
        return trimmed;
    }

    private static String normalizeCreatedBy(String createdBy) {
        if (createdBy == null || createdBy.isBlank()) return null;
        var trimmed = createdBy.trim();
        if (trimmed.length() > CREATED_BY_MAX_LENGTH) throw new IllegalArgumentException("product.error.createdBy.size");
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public Sku getSku() {
        return sku;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
