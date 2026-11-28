package com.safezone.nodesecure.platform.subscriptions.application.internal.services;

import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** T26: read-only catalog of SaaS plans. */
@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanService(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    /** How a request names a plan: by id, by code (BASIC) or by name (Básico, kept for older clients). */
    public record PlanReference(Long planId, String planCode, String planName) {
        public boolean isEmpty() {
            return planId == null && isBlank(planCode) && isBlank(planName);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> findAll() {
        return repository.findAllByActiveTrueOrderByMonthlyPriceAsc();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("plans.error.notFound", id));
    }

    /**
     * Catalog plan a subscription was contracted with, even if the plan was later retired.
     * Subscriptions created before the catalog have no plan id and are matched by name.
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionPlan> findContracted(Long planId, String planName) {
        if (planId != null) {
            return repository.findById(planId);
        }
        return planName == null || planName.isBlank()
                ? Optional.empty()
                : repository.findByNameIgnoreCaseAndActiveTrue(planName.trim());
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan resolve(PlanReference reference) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("subscriptions.error.plan.required");
        }
        if (reference.planId() != null) {
            return getById(reference.planId());
        }
        String key = !PlanReference.isBlank(reference.planCode()) ? reference.planCode().trim() : reference.planName().trim();
        return repository.findByCodeIgnoreCaseAndActiveTrue(key)
                .or(() -> repository.findByNameIgnoreCaseAndActiveTrue(key))
                .orElseThrow(() -> new ResourceNotFoundException("plans.error.notFound", key));
    }
}
