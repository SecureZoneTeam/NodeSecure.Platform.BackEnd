package com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.InvoiceConcept;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.Money;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanLimits;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.TaxId;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionAggregatesTest {

    private static final Instant NOW = Instant.parse("2026-01-31T10:00:00Z");

    private static SubscriptionPlan plan(long id, String code, String name, String price, int warehouses) {
        SubscriptionPlan plan = SubscriptionPlan.define(code, name, new Money(new BigDecimal(price), "USD"),
                new PlanLimits(warehouses, 5, 3));
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    private final SubscriptionPlan basic = plan(1, "basic", "Básico", "49", 1);
    private final SubscriptionPlan premium = plan(2, "PREMIUM", "Premium", "129", 5);

    @Test
    void planNormalizesCodeAndExposesPriceAndLimits() {
        assertThat(basic.getCode()).isEqualTo("BASIC");
        assertThat(basic.price()).isEqualTo(new Money(new BigDecimal("49.00"), "USD"));
        assertThat(basic.limits()).isEqualTo(new PlanLimits(1, 5, 3));
        assertThat(basic.isActive()).isTrue();
        assertThatThrownBy(() -> SubscriptionPlan.define("1", "X", basic.price(), basic.limits()))
                .hasMessage("plans.error.code.invalid");
    }

    @Test
    void activationCopiesThePlanAndStartsAMonthlyPeriod() {
        Subscription subscription = Subscription.activate(7L, basic, NOW);
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getCompanyId()).isEqualTo(7L);
        assertThat(subscription.getPlanId()).isEqualTo(1L);
        assertThat(subscription.getPlanName()).isEqualTo("Básico");
        assertThat(subscription.getPrice()).isEqualTo(49.0);
        assertThat(subscription.getCurrency()).isEqualTo("USD");
        assertThat(subscription.getMaxWarehouses()).isEqualTo(1);
        assertThat(subscription.getStartedAt()).isEqualTo(NOW);
        assertThat(subscription.getRenewsAt()).isEqualTo(Instant.parse("2026-02-28T10:00:00Z"));
    }

    @Test
    void changePlanRequiresAnotherPlan() {
        Subscription subscription = Subscription.activate(7L, basic, NOW);
        assertThatThrownBy(() -> subscription.changePlan(basic, NOW)).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("subscriptions.error.samePlan");
        subscription.changePlan(premium, NOW.plusSeconds(60));
        assertThat(subscription.getPlanId()).isEqualTo(2L);
        assertThat(subscription.getPrice()).isEqualTo(129.0);
        assertThat(subscription.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void cancelledIsFinal() {
        Subscription subscription = Subscription.activate(7L, basic, NOW);
        subscription.cancel(NOW);
        assertThat(subscription.getStatus()).isEqualTo("CANCELLED");
        assertThat(subscription.getCancelledAt()).isEqualTo(NOW);
        assertThat(subscription.getRenewsAt()).isNull();
        assertThatThrownBy(() -> subscription.cancel(NOW)).hasMessage("subscriptions.error.notActive");
        assertThatThrownBy(() -> subscription.changePlan(premium, NOW)).hasMessage("subscriptions.error.notActive");
    }

    @Test
    void invoiceBillsThePlanPriceForOnePeriod() {
        Subscription subscription = Subscription.activate(7L, premium, NOW);
        ReflectionTestUtils.setField(subscription, "id", 10L);
        Invoice invoice = Invoice.issue(subscription, premium, InvoiceConcept.ACTIVATION, NOW);
        assertThat(invoice.getCompanyId()).isEqualTo(7L);
        assertThat(invoice.getSubscriptionId()).isEqualTo(10L);
        assertThat(invoice.getPlanId()).isEqualTo(2L);
        assertThat(invoice.getAmount()).isEqualByComparingTo("129.00");
        assertThat(invoice.getCurrency()).isEqualTo("USD");
        assertThat(invoice.getStatus()).isEqualTo("ISSUED");
        assertThat(invoice.getConcept()).isEqualTo("ACTIVATION");
        assertThat(invoice.getPeriodStart()).isEqualTo(NOW);
        assertThat(invoice.getPeriodEnd()).isEqualTo(subscription.getRenewsAt());
    }

    @Test
    void companyNormalizesItsData() {
        Company company = Company.register(7L, "  Andina S.A.C. ", new TaxId("20131312955"), "Contacto@Andina.PE", null, NOW);
        assertThat(company.getLegalName()).isEqualTo("Andina S.A.C.");
        assertThat(company.getContactEmail()).isEqualTo("contacto@andina.pe");
        assertThat(company.getCountry()).isEqualTo("PE");
        assertThatThrownBy(() -> Company.register(7L, " ", new TaxId("20131312955"), "a@b.pe", "PE", NOW))
                .hasMessage("companies.error.legalName.invalid");
    }
}
