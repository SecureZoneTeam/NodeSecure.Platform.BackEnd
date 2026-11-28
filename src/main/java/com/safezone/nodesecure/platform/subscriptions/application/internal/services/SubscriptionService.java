package com.safezone.nodesecure.platform.subscriptions.application.internal.services;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.sensors.interfaces.acl.SensorContextFacade;
import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionPlanService.PlanReference;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Invoice;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.InvoiceConcept;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.OrganizationUsage;
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

    private void requireUsageFits(Long companyId, SubscriptionPlan plan) {
        OrganizationUsage usage = new OrganizationUsage(
                warehouseFacade.findWarehouseIdsByCompanyId(companyId).size(),
                sensorFacade.countSensorNodes(companyId),
                iamFacade.countUsers(companyId));
        List<String> violations = plan.limits().violationsFor(usage);
        if (!violations.isEmpty()) {
            throw new BusinessRuleViolationException("subscriptions.error.limitsExceeded", plan.getName(),
                    String.join(", ", violations));
        }
    }

    private Invoice issueInvoice(Subscription subscription, SubscriptionPlan plan, InvoiceConcept concept, Instant now) {
        if (invoices.existsBySubscriptionIdAndPeriodStart(subscription.getId(), now)) {
            throw new BusinessRuleViolationException("invoices.error.duplicated", subscription.getId());
        }
        return invoices.save(Invoice.issue(subscription, plan, concept, now));
    }
}
