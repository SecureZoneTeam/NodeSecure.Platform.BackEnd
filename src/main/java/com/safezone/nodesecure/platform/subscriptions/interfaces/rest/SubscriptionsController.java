package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Subscriptions of the signed-in owner's organization. Billing data is owner-only
 * (subscriptions.view / subscriptions.manage) and never crosses organizations.
 * No payment gateway is integrated in the backend.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscriptions of my organization")
public class SubscriptionsController {

    private final SubscriptionRepository subscriptionRepository;
    private final IamContextFacade iamFacade;

    public SubscriptionsController(SubscriptionRepository subscriptionRepository, IamContextFacade iamFacade) {
        this.subscriptionRepository = subscriptionRepository;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Subscriptions of my organization")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping
    public ResponseEntity<List<SubscriptionResource>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionRepository.findAllByCompanyIdOrderByIdAsc(iamFacade.requireCompanyId())
                .stream().map(SubscriptionResource::from).toList());
    }

    @Operation(summary = "Register a subscription for my organization")
    @PreAuthorize("hasAuthority('subscriptions.manage')")
    @PostMapping
    public ResponseEntity<SubscriptionResource> createSubscription(@Valid @RequestBody CreateSubscriptionResource resource) {
        Subscription subscription = new Subscription(resource.planName().trim(), resource.price(),
                resource.maxWarehouses(), resource.maxSensors(),
                resource.status() == null || resource.status().isBlank() ? "ACTIVE" : resource.status().trim());
        subscription.setCompanyId(iamFacade.requireCompanyId());
        Subscription saved = subscriptionRepository.save(subscription);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResource.from(saved));
    }

    public record CreateSubscriptionResource(
            @NotBlank String planName,
            @NotNull @PositiveOrZero Double price,
            @NotNull @PositiveOrZero Integer maxWarehouses,
            @NotNull @PositiveOrZero Integer maxSensors,
            String status) {
    }

    public record SubscriptionResource(Long id, Long companyId, String planName, Double price,
                                       Integer maxWarehouses, Integer maxSensors, String status) {
        static SubscriptionResource from(Subscription subscription) {
            return new SubscriptionResource(subscription.getId(), subscription.getCompanyId(),
                    subscription.getPlanName(), subscription.getPrice(), subscription.getMaxWarehouses(),
                    subscription.getMaxSensors(), subscription.getStatus());
        }
    }
}
