package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.MacAddress;
import com.safezone.nodesecure.platform.sensors.domain.repositories.SensorNodeRepository;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.mappers.SensorNodePersistenceMapper;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.repositories.SensorNodeJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SensorNodeRepositoryAdapter implements SensorNodeRepository {
    private final SensorNodeJpaRepository jpa;
    private final SensorNodePersistenceMapper mapper;

    public SensorNodeRepositoryAdapter(SensorNodeJpaRepository jpa, SensorNodePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override public SensorNode save(SensorNode node) { return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(node))); }
    @Override public Optional<SensorNode> findByIdAndCompanyId(Long id, Long companyId) { return jpa.findByIdAndCompanyId(id, companyId).map(mapper::toDomain); }
    @Override public Optional<SensorNode> findByMacAddress(MacAddress macAddress) { return jpa.findByMacAddress(macAddress.value()).map(mapper::toDomain); }
    @Override public List<SensorNode> findAllByCompanyId(Long companyId) { return jpa.findAllByCompanyIdOrderByIdAsc(companyId).stream().map(mapper::toDomain).toList(); }
    @Override public boolean existsByMacAddress(MacAddress macAddress) { return jpa.existsByMacAddress(macAddress.value()); }
    @Override public boolean existsByMacAddressAndIdIsNot(MacAddress macAddress, Long id) { return jpa.existsByMacAddressAndIdIsNot(macAddress.value(), id); }
    @Override public boolean existsByZoneId(Long zoneId) { return jpa.existsByZoneId(zoneId); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
