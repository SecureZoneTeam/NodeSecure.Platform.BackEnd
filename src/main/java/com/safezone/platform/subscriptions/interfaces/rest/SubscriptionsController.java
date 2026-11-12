package com.safezone.platform.subscriptions.interfaces.rest;

import com.safezone.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.platform.subscriptions.domain.repositories.SubscriptionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Available SaaS Subscription & Billing Endpoints")
@CrossOrigin(origins = "*")
public class SubscriptionsController {
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionsController(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Subscription> createSubscription(@RequestBody Subscription subscription) {
        Subscription saved = subscriptionRepository.save(subscription);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
