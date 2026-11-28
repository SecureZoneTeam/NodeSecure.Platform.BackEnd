package com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Subscription of an organization to a catalog plan. Plan name, price and limits are kept as a
 * snapshot of the plan at activation or last change (columns that already existed in the table).
 * Rows created before the plan catalog have no plan_id and no dates; they keep working.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Organization (tenant). Nullable only for rows created before the multi-tenant migration. */
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private Double price;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer maxWarehouses;

    @Column(nullable = false)
    private Integer maxSensors;

    @Column(nullable = false)
    private String status;

    private Instant startedAt;

    private Instant renewsAt;

    private Instant cancelledAt;

    private Instant updatedAt;

    public static Subscription activate(Long companyId, SubscriptionPlan plan, Instant now) {
        Subscription subscription = new Subscription();
        subscription.companyId = Objects.requireNonNull(companyId);
        subscription.status = SubscriptionStatus.ACTIVE.name();
        subscription.startedAt = now;
        subscription.applyPlan(plan, now);
        return subscription;
    }

    public boolean isActive() {
        return SubscriptionStatus.ACTIVE.name().equalsIgnoreCase(status);
    }

    /** ACTIVE -> ACTIVE with another plan; a new monthly period starts now. */
    public void changePlan(SubscriptionPlan plan, Instant now) {
        requireActive();
        boolean samePlan = planId != null ? planId.equals(plan.getId()) : plan.getName().equalsIgnoreCase(planName);
        if (samePlan) {
            throw new BusinessRuleViolationException("subscriptions.error.samePlan", plan.getName());
        }
        applyPlan(plan, now);
    }

    /** ACTIVE -> CANCELLED. A cancelled subscription cannot be changed or cancelled again. */
    public void cancel(Instant now) {
        requireActive();
        status = SubscriptionStatus.CANCELLED.name();
        cancelledAt = now;
        renewsAt = null;
        updatedAt = now;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new BusinessRuleViolationException("subscriptions.error.notActive", id, status);
        }
    }

    private void applyPlan(SubscriptionPlan plan, Instant now) {
        planId = plan.getId();
        planName = plan.getName();
        price = plan.getMonthlyPrice().doubleValue();
        currency = plan.getCurrency();
        maxWarehouses = plan.getWarehouseLimit();
        maxSensors = plan.getSensorNodeLimit();
        renewsAt = nextPeriod(now);
        updatedAt = now;
    }

    /** End of a monthly period that starts at {@code from} (UTC calendar month). */
    public static Instant nextPeriod(Instant from) {
        return from.atOffset(ZoneOffset.UTC).plusMonths(1).toInstant();
    }
}
