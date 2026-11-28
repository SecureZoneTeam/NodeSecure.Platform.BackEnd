package com.safezone.nodesecure.platform.subscriptions.application.acl;

import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionService;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanResource;
import com.safezone.nodesecure.platform.subscriptions.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionContextFacadeImpl implements SubscriptionContextFacade {

    private final SubscriptionService subscriptionService;

    public SubscriptionContextFacadeImpl(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void requireWarehouseCapacity(Long companyId) {
        subscriptionService.requireCapacity(companyId, PlanResource.WAREHOUSES);
    }

    @Override
    public void requireSensorNodeCapacity(Long companyId) {
        subscriptionService.requireCapacity(companyId, PlanResource.SENSOR_NODES);
    }

    @Override
    public void requireUserCapacity(Long companyId) {
        subscriptionService.requireCapacity(companyId, PlanResource.USERS);
    }
}
