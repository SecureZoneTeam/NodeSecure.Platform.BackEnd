package com.safezone.nodesecure.platform.subscriptions.infrastructure.seed;

import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.Money;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanLimits;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.SubscriptionPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inserts the plans of the catalog (US15: Básico, Premium, Corporativo) that are missing.
 * Existing plans are never modified. Same values as database/saas_traceability_reporting.sql.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "subscriptions.seed-plans", havingValue = "true", matchIfMissing = true)
public class SubscriptionPlanSeeder implements ApplicationRunner {

    static final List<SubscriptionPlan> CATALOG = List.of(
            SubscriptionPlan.define("BASIC", "Básico", new Money(new BigDecimal("49.00"), "USD"), new PlanLimits(1, 5, 3)),
            SubscriptionPlan.define("PREMIUM", "Premium", new Money(new BigDecimal("129.00"), "USD"), new PlanLimits(5, 30, 15)),
            SubscriptionPlan.define("CORPORATE", "Corporativo", new Money(new BigDecimal("349.00"), "USD"), new PlanLimits(25, 200, 100)));

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanSeeder(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CATALOG.stream()
                .filter(plan -> !repository.existsByCodeIgnoreCase(plan.getCode()))
                .forEach(plan -> {
                    repository.save(SubscriptionPlan.define(plan.getCode(), plan.getName(), plan.price(), plan.limits()));
                    log.info("Subscription plan {} added to the catalog", plan.getCode());
                });
    }
}
