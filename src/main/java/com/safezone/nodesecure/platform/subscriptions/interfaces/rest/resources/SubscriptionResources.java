package com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources;

import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionService;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanResource;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Company;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Invoice;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Request and response resources of the Companies, Plans, Subscriptions and Invoices endpoints. */
public final class SubscriptionResources {

    private SubscriptionResources() {
    }

    @Schema(description = "Legal data of my organization")
    public record CreateCompanyResource(
            @Schema(example = "Distribuidora Andina S.A.C.") @NotBlank @Size(max = 150) String legalName,
            @Schema(example = "20131312955", description = "RUC: 11 digits with a valid SUNAT check digit")
            @NotBlank @Pattern(regexp = "\\d{11}") String taxId,
            @Schema(example = "contacto@andina.pe") @NotBlank @Email @Size(max = 120) String contactEmail,
            @Schema(example = "PE") @Pattern(regexp = "[A-Za-z]{2}") String country) {
    }

    public record CompanyResource(Long id, Long organizationId, String legalName, String taxId, String contactEmail,
                                  String country, Long subscriptionId, Instant createdAt) {
        public static CompanyResource from(Company company, Long subscriptionId) {
            return new CompanyResource(company.getId(), company.getCompanyId(), company.getLegalName(),
                    company.getTaxId(), company.getContactEmail(), company.getCountry(), subscriptionId,
                    company.getCreatedAt());
        }
    }

    public record SubscriptionPlanResource(Long id, String code, String name, BigDecimal monthlyPrice, String currency,
                                           Integer warehouseLimit, Integer sensorNodeLimit, Integer userLimit) {
        public static SubscriptionPlanResource from(SubscriptionPlan plan) {
            return new SubscriptionPlanResource(plan.getId(), plan.getCode(), plan.getName(), plan.getMonthlyPrice(),
                    plan.getCurrency(), plan.getWarehouseLimit(), plan.getSensorNodeLimit(), plan.getUserLimit());
        }
    }

    @Schema(description = "Plan to activate. Use planId or planCode; planName is accepted for older clients. "
            + "Price and limits always come from the catalog: price, maxWarehouses and maxSensors are ignored.")
    public record CreateSubscriptionResource(
            @Schema(example = "2") @Positive Long planId,
            @Schema(example = "PREMIUM") String planCode,
            @Schema(example = "Premium") String planName,
            @Schema(hidden = true) Double price,
            @Schema(hidden = true) Integer maxWarehouses,
            @Schema(hidden = true) Integer maxSensors,
            @Schema(description = "Optional; only ACTIVE is allowed", example = "ACTIVE") String status) {
    }

    @Schema(description = "New plan of an active subscription")
    public record ChangePlanResource(@Schema(example = "3") @Positive Long planId,
                                     @Schema(example = "CORPORATE") String planCode) {
    }

    public record SubscriptionResource(Long id, Long companyId, Long planId, String planName, Double price,
                                       String currency, Integer maxWarehouses, Integer maxSensors, String status,
                                       Instant startedAt, Instant renewsAt, Instant cancelledAt,
                                       Long latestInvoiceId) {
        public static SubscriptionResource from(Subscription subscription) {
            return from(subscription, null);
        }

        public static SubscriptionResource from(Subscription subscription, Invoice invoice) {
            return new SubscriptionResource(subscription.getId(), subscription.getCompanyId(), subscription.getPlanId(),
                    subscription.getPlanName(), subscription.getPrice(), subscription.getCurrency(),
                    subscription.getMaxWarehouses(), subscription.getMaxSensors(), subscription.getStatus(),
                    subscription.getStartedAt(), subscription.getRenewsAt(), subscription.getCancelledAt(),
                    invoice == null ? null : invoice.getId());
        }
    }

    public record ResourceUsage(String resource, long used, Integer limit) {
    }

    @Schema(description = "Active subscription of my organization with the usage of each limited resource")
    public record SubscriptionUsageResource(Long subscriptionId, String planCode, String planName, Double price,
                                            String currency, String status, Instant startedAt, Instant renewsAt,
                                            List<ResourceUsage> resources) {
        public static SubscriptionUsageResource from(SubscriptionService.UsageReport report) {
            Subscription subscription = report.subscription();
            List<ResourceUsage> resources = Arrays.stream(PlanResource.values())
                    .map(resource -> {
                        int limit = report.limits().of(resource);
                        return new ResourceUsage(resource.key(), report.usage().of(resource),
                                limit == Integer.MAX_VALUE ? null : limit);
                    })
                    .toList();
            return new SubscriptionUsageResource(subscription.getId(), report.planCode(), subscription.getPlanName(),
                    subscription.getPrice(), subscription.getCurrency(), subscription.getStatus(),
                    subscription.getStartedAt(), subscription.getRenewsAt(), resources);
        }
    }

    public record InvoiceResource(Long id, Long companyId, Long subscriptionId, Long planId, String concept,
                                  BigDecimal amount, String currency, String status, Instant issuedAt,
                                  Instant periodStart, Instant periodEnd) {
        public static InvoiceResource from(Invoice invoice) {
            return new InvoiceResource(invoice.getId(), invoice.getCompanyId(), invoice.getSubscriptionId(),
                    invoice.getPlanId(), invoice.getConcept(), invoice.getAmount(), invoice.getCurrency(),
                    invoice.getStatus(), invoice.getIssuedAt(), invoice.getPeriodStart(), invoice.getPeriodEnd());
        }
    }
}
