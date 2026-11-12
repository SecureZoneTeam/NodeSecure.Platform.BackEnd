package com.safezone.nodesecure.platform.sensors.domain.repositories;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;

import java.util.List;

public interface PhysicalEventRepository {
    PhysicalEvent save(PhysicalEvent event);
    List<PhysicalEvent> findAllByCompanyId(Long companyId);
    boolean existsBySensorNodeId(Long sensorNodeId);
}
