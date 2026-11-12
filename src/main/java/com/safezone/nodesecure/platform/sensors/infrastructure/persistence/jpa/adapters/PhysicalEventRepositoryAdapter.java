package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.domain.repositories.PhysicalEventRepository;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.mappers.PhysicalEventPersistenceMapper;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.repositories.PhysicalEventJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PhysicalEventRepositoryAdapter implements PhysicalEventRepository {
    private final PhysicalEventJpaRepository jpa;
    private final PhysicalEventPersistenceMapper mapper;

    public PhysicalEventRepositoryAdapter(PhysicalEventJpaRepository jpa, PhysicalEventPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override public PhysicalEvent save(PhysicalEvent event) { return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(event))); }
    @Override public List<PhysicalEvent> findAllByCompanyId(Long companyId) { return jpa.findAllByCompanyIdOrderByOccurredAtDesc(companyId).stream().map(mapper::toDomain).toList(); }
    @Override public boolean existsBySensorNodeId(Long sensorNodeId) { return jpa.existsBySensorNodeId(sensorNodeId); }
}
