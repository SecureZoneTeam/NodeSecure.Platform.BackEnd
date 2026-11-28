package com.safezone.nodesecure.platform.subscriptions.application.internal.services;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.sensors.interfaces.acl.SensorContextFacade;
import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionPlanService.PlanReference;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Invoice;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import com.safezone.nodesecure.platform.subscriptions.domain.exceptions.PlanLimitExceededException;
import com.safezone.nodesecure.platform.subscriptions.domain.exceptions.SubscriptionRequiredException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.InvoiceConcept;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.LimitViolation;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.OrganizationUsage;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanLimits;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanResource;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.SubscriptionStatus;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.InvoiceRepository;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.SubscriptionRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * T27 + T28: activation, plan change and cancellation of the organization's subscription.
 * Activations and plan changes issue their invoice in the same transaction: either both are
 * stored or neither is.
 */
@Service
public class SubscriptionService {

    private static final String ACTIVE = SubscriptionStatus.ACTIVE.name();

    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final SubscriptionPlanService plans;
    private final WarehouseContextFacade warehouseFacade;
    private final SensorContextFacade sensorFacade;
    private final IamContextFacade iamFacade;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptions, InvoiceRepository invoices,
                               SubscriptionPlanService plans, WarehouseContextFacade warehouseFacade,
                               SensorContextFacade sensorFacade, IamContextFacade iamFacade, Clock clock) {
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.plans = plans;
        this.warehouseFacade = warehouseFacade;
        this.sensorFacade = sensorFacade;
        this.iamFacade = iamFacade;
        this.clock = clock;
    }

    public record ActivationResult(Subscription subscription, Invoice invoice) {
    }

    @Transactional
    public ActivationResult activate(Long companyId, PlanReference reference) {
        SubscriptionPlan plan = plans.resolve(reference);
        subscriptions.lockOrganization(companyId);
        if (subscriptions.existsByCompanyIdAndStatus(companyId, ACTIVE)) {
            throw new BusinessRuleViolationException("subscriptions.error.alreadyActive");
        }
        requireUsageFits(companyId, plan);
        Instant now = clock.instant();
        Subscription saved = subscriptions.save(Subscription.activate(companyId, plan, now));
        return new ActivationResult(saved, issueInvoice(saved, plan, InvoiceConcept.ACTIVATION, now));
    }

    @Transactional
    public ActivationResult changePlan(Long companyId, Long subscriptionId, PlanReference reference) {
        SubscriptionPlan plan = plans.resolve(reference);
        subscriptions.lockOrganization(companyId);
        Subscription subscription = getById(subscriptionId, companyId);
        requireUsageFits(companyId, plan);
        Instant now = clock.instant();
        subscription.changePlan(plan, now);
        Subscription saved = subscriptions.save(subscription);
        return new ActivationResult(saved, issueInvoice(saved, plan, InvoiceConcept.PLAN_CHANGE, now));
    }

    @Transactional
    public Subscription cancel(Long companyId, Long subscriptionId) {
        subscriptions.lockOrganization(companyId);
        Subscription subscription = getById(subscriptionId, companyId);
        subscription.cancel(clock.instant());
        return subscriptions.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Subscription> findAll(Long companyId) {
        return subscriptions.findAllByCompanyIdOrderByIdAsc(companyId);
    }

    @Transactional(readOnly = true)
    public Subscription getById(Long id, Long companyId) {
        return subscriptions.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("subscriptions.error.notFound", id));
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findActive(Long companyId) {
        return subscriptions.findFirstByCompanyIdAndStatusOrderByIdDesc(companyId, ACTIVE);
    }

    /** Usage of the organization compared with the limits of its active subscription. */
    public record UsageReport(Subscription subscription, String planCode, OrganizationUsage usage, PlanLimits limits) {
    }

    @Transactional(readOnly = true)
    public Optional<UsageReport> usage(Long companyId) {
        return findActive(companyId).map(active -> {
            Optional<SubscriptionPlan> plan = plans.findContracted(active.getPlanId(), active.getPlanName());
            return new UsageReport(active, plan.map(SubscriptionPlan::getCode).orElse(null),
                    currentUsage(companyId), contractedLimits(active, plan));
        });
    }

    /**
     * Rejects the creation of one more resource when the organization has no active subscription
     * or already uses everything its plan allows. Must run inside the creating transaction: the
     * organization row stays locked until it ends, so concurrent creations are serialized.
     */
    @Transactional
    public void requireCapacity(Long companyId, PlanResource resource) {
        subscriptions.lockOrganization(companyId);
        Subscription active = findActive(companyId).orElseThrow(() -> new SubscriptionRequiredException(resource));
        Optional<SubscriptionPlan> plan = plans.findContracted(active.getPlanId(), active.getPlanName());
        PlanLimits limits = contractedLimits(active, plan);
        long used = count(companyId, resource);
        if (!limits.allowsOneMore(resource, used)) {
            throw PlanLimitExceededException.limitReached(plan.map(SubscriptionPlan::getCode).orElse(null),
                    active.getPlanName(), new LimitViolation(resource, used, limits.of(resource)));
        }
    }

    /**
     * Limits the organization paid for: warehouses and sensors come from the subscription snapshot;
     * the user limit from its catalog plan (subscriptions created before the catalog have no user limit).
     */
    private static PlanLimits contractedLimits(Subscription subscription, Optional<SubscriptionPlan> plan) {
        int users = plan.map(SubscriptionPlan::getUserLimit).orElse(Integer.MAX_VALUE);
        return new PlanLimits(subscription.getMaxWarehouses(), subscription.getMaxSensors(), users);
    }

    private OrganizationUsage currentUsage(Long companyId) {
        return new OrganizationUsage(count(companyId, PlanResource.WAREHOUSES),
                count(companyId, PlanResource.SENSOR_NODES), count(companyId, PlanResource.USERS));
    }

    private long count(Long companyId, PlanResource resource) {
        return switch (resource) {
            case WAREHOUSES -> warehouseFacade.findWarehouseIdsByCompanyId(companyId).size();
            case SENSOR_NODES -> sensorFacade.countSensorNodes(companyId);
            case USERS -> iamFacade.countUsers(companyId);
        };
    }

    private void requireUsageFits(Long companyId, SubscriptionPlan plan) {
        List<LimitViolation> violations = plan.limits().exceededBy(currentUsage(companyId));
        if (!violations.isEmpty()) {
            throw PlanLimitExceededException.usageExceedsPlan(plan.getCode(), plan.getName(), violations);
        }
    }

    private Invoice issueInvoice(Subscription subscription, SubscriptionPlan plan, InvoiceConcept concept, Instant now) {
        if (invoices.existsBySubscriptionIdAndPeriodStart(subscription.getId(), now)) {
            throw new BusinessRuleViolationException("invoices.error.duplicated", subscription.getId());
        }
        return invoices.save(Invoice.issue(subscription, plan, concept, now));
    }
}
