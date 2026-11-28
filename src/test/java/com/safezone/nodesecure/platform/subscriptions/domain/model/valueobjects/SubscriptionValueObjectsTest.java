package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionValueObjectsTest {

    @ParameterizedTest
    @ValueSource(strings = {"20131312955", "20100047218", " 20131312955 "})
    void acceptsRucWithValidPrefixAndCheckDigit(String ruc) {
        assertThat(new TaxId(ruc).value()).isEqualTo(ruc.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {"20512345678", "2013131295", "201313129551", "30131312955", "2013131295A", ""})
    void rejectsInvalidRuc(String ruc) {
        assertThatThrownBy(() -> new TaxId(ruc)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullRuc() {
        assertThatThrownBy(() -> new TaxId(null)).hasMessage("companies.error.taxId.notBlank");
    }

    @Test
    void moneyNormalizesScaleAndCurrency() {
        Money money = new Money(new BigDecimal("49"), "usd");
        assertThat(money.amount()).isEqualByComparingTo("49.00");
        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.currency()).isEqualTo("USD");
    }

    @Test
    void moneyRejectsNegativeAmountsAndInvalidCurrencies() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "USD")).hasMessage("subscriptions.error.money.amount");
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "US")).hasMessage("subscriptions.error.money.currency");
    }

    @Test
    void planLimitsMustBePositive() {
        assertThatThrownBy(() -> new PlanLimits(0, 5, 3)).hasMessage("subscriptions.error.limits.positive");
    }

    @Test
    void planLimitsReportEveryExceededResource() {
        PlanLimits basic = new PlanLimits(1, 5, 3);
        assertThat(basic.violationsFor(new OrganizationUsage(1, 5, 3))).isEmpty();
        assertThat(basic.violationsFor(new OrganizationUsage(2, 6, 4)))
                .containsExactly("warehouses 2/1", "sensorNodes 6/5", "users 4/3");
    }
}
