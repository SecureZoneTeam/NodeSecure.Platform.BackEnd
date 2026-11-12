package com.safezone.nodesecure.platform.sensors.interfaces.rest.transform;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.PhysicalEventResource;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.SensorNodeResource;

public final class SensorResourceMapper {
    private SensorResourceMapper() { }

    public static SensorNodeResource toResource(SensorNode node) {
        return new SensorNodeResource(node.getId(), node.getZoneId(), node.getMacAddress().value(), node.getModel(),
                node.getSensorType(), node.getConnectionStatus(), node.getLastPingAt());
    }

    public static PhysicalEventResource toResource(PhysicalEvent event) {
        return new PhysicalEventResource(event.getId(), event.getSensorNodeId(), event.getZoneId(),
                event.getEventType(), event.getOccurredAt(), event.isReconciled());
    }
}
