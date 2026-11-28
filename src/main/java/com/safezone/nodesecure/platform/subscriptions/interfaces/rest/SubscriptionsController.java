package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionPlanService.PlanReference;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionService;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionService.ActivationResult;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.ChangePlanResource;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.CreateSubscriptionResource;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.SubscriptionResource;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.SubscriptionUsageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * T27: subscriptions of the signed-in owner's organization. Billing data is owner-only
 * (subscriptions.view / subscriptions.manage) and never crosses organizations.
 * No payment gateway is integrated in the backend: activation issues an invoice (T28).
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Activate, change plan and cancel the subscription of my organization")
public class SubscriptionsController {

    private final SubscriptionService subscriptionService;
    private final IamContextFacade iamFacade;

    public SubscriptionsController(SubscriptionService subscriptionService, IamContextFacade iamFacade) {
        this.subscriptionService = subscriptionService;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Subscriptions of my organization (history included)")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping
    public ResponseEntity<List<SubscriptionResource>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.findAll(iamFacade.requireCompanyId())
                .stream().map(SubscriptionResource::from).toList());
    }

    @Operation(summary = "Active subscription of my organization")
    @ApiResponse(responseCode = "404", description = "The organization has no active subscription")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping("/current")
    public ResponseEntity<SubscriptionResource> getCurrent() {
        return subscriptionService.findActive(iamFacade.requireCompanyId())
                .map(subscription -> ResponseEntity.ok(SubscriptionResource.from(subscription)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Usage of my organization against the limits of its active subscription",
            description = "Warehouses, sensor nodes and users used and contracted. A null limit means not limited.")
    @ApiResponse(responseCode = "404", description = "The organization has no active subscription")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping("/current/usage")
    public ResponseEntity<SubscriptionUsageResource> getCurrentUsage() {
        return subscriptionService.usage(iamFacade.requireCompanyId())
                .map(report -> ResponseEntity.ok(SubscriptionUsageResource.from(report)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get a subscription of my organization")
    @ApiResponse(responseCode = "404", description = "Not found in my organization")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(SubscriptionResource.from(subscriptionService.getById(id, iamFacade.requireCompanyId())));
    }

    @Operation(summary = "Activate a plan for my organization",
            description = "Price and limits come from the plan catalog. Issues the invoice of the first period.")
    @ApiResponse(responseCode = "201", description = "Subscription activated and invoice issued")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @ApiResponse(responseCode = "409", description = "Already has an active subscription or current usage exceeds the plan limits")
    @PreAuthorize("hasAuthority('subscriptions.manage')")
    @PostMapping
    public ResponseEntity<SubscriptionResource> createSubscription(@Valid @RequestBody CreateSubscriptionResource resource) {
        if (resource.status() != null && !resource.status().isBlank() && !"ACTIVE".equalsIgnoreCase(resource.status().trim())) {
            throw new IllegalArgumentException("subscriptions.error.status.invalid");
        }
        ActivationResult result = subscriptionService.activate(iamFacade.requireCompanyId(),
                new PlanReference(resource.planId(), resource.planCode(), resource.planName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResource.from(result.subscription(), result.invoice()));
    }

    @Operation(summary = "Change the plan of an active subscription",
            description = "Starts a new monthly period and issues its invoice. Rejected when the current usage does not fit in the new plan.")
    @ApiResponse(responseCode = "409", description = "Subscription not active, same plan or usage exceeds the new plan limits")
    @PreAuthorize("hasAuthority('subscriptions.manage')")
    @PatchMapping("/{id}/plan")
    public ResponseEntity<SubscriptionResource> changePlan(@PathVariable Long id, @Valid @RequestBody ChangePlanResource resource) {
        ActivationResult result = subscriptionService.changePlan(iamFacade.requireCompanyId(), id,
                new PlanReference(resource.planId(), resource.planCode(), null));
        return ResponseEntity.ok(SubscriptionResource.from(result.subscription(), result.invoice()));
    }

    @Operation(summary = "Cancel an active subscription")
    @ApiResponse(responseCode = "409", description = "The subscription is not active")
    @PreAuthorize("hasAuthority('subscriptions.manage')")
    @PostMapping("/{id}/cancellation")
    public ResponseEntity<SubscriptionResource> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(SubscriptionResource.from(subscriptionService.cancel(iamFacade.requireCompanyId(), id)));
    }
}
