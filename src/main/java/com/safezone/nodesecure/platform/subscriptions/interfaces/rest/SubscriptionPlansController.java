package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionPlanService;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.SubscriptionPlanResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** T26: public, read-only catalog of SaaS plans (US15: visitors compare plans on the landing page). */
@RestController
@RequestMapping(value = "/api/v1/subscription-plans", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Subscription Plans", description = "Catalog of SaaS plans (public)")
public class SubscriptionPlansController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlansController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Plans with monthly price and limits of warehouses, sensor nodes and users")
    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResource>> getAll() {
        return ResponseEntity.ok(planService.findAll().stream().map(SubscriptionPlanResource::from).toList());
    }

    @Operation(summary = "Get a plan by id")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(SubscriptionPlanResource.from(planService.getById(id)));
    }
}
