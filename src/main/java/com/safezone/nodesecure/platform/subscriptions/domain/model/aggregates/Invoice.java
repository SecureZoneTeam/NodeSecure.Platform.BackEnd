package com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates;

import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.InvoiceConcept;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.InvoiceStatus;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.Money;
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
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Invoice issued for a monthly period of a subscription. One invoice per subscription and period
 * start (unique key), so the same activation or plan change can never be billed twice.
 */
@Entity
@Immutable
@Table(name = "invoices", uniqueConstraints =
        @UniqueConstraint(name = "uk_invoices_subscription_period", columnNames = {"subscription_id", "period_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false, length = 20)
    private String concept;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(nullable = false)
    private Instant periodEnd;

    public static Invoice issue(Subscription subscription, SubscriptionPlan plan, InvoiceConcept concept, Instant now) {
        Money price = plan.price();
        Invoice invoice = new Invoice();
        invoice.companyId = subscription.getCompanyId();
        invoice.subscriptionId = subscription.getId();
        invoice.planId = plan.getId();
        invoice.concept = concept.name();
        invoice.amount = price.amount();
        invoice.currency = price.currency();
        invoice.status = InvoiceStatus.ISSUED.name();
        invoice.issuedAt = now;
        invoice.periodStart = now;
        invoice.periodEnd = Subscription.nextPeriod(now);
        return invoice;
    }
}
