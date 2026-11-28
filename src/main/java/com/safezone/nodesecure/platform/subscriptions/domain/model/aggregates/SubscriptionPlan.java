package com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates;

import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.Money;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanLimits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Locale;

/** SaaS plan of the catalog: monthly price and operational limits (warehouses, sensor nodes, users). */
@Entity
@Table(name = "subscription_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_plans_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_subscription_plans_name", columnNames = "name")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer warehouseLimit;

    @Column(nullable = false)
    private Integer sensorNodeLimit;

    @Column(nullable = false)
    private Integer userLimit;

    @Column(nullable = false)
    private boolean active;

    public static SubscriptionPlan define(String code, String name, Money price, PlanLimits limits) {
        if (code == null || !code.trim().matches("[A-Za-z_]{2,30}")) {
            throw new IllegalArgumentException("plans.error.code.invalid");
        }
        if (name == null || name.isBlank() || name.trim().length() > 60) {
            throw new IllegalArgumentException("plans.error.name.invalid");
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.code = code.trim().toUpperCase(Locale.ROOT);
        plan.name = name.trim();
        plan.monthlyPrice = price.amount();
        plan.currency = price.currency();
        plan.warehouseLimit = limits.warehouses();
        plan.sensorNodeLimit = limits.sensorNodes();
        plan.userLimit = limits.users();
        plan.active = true;
        return plan;
    }

    public Money price() {
        return new Money(monthlyPrice, currency);
    }

    public PlanLimits limits() {
        return new PlanLimits(warehouseLimit, sensorNodeLimit, userLimit);
    }
}
