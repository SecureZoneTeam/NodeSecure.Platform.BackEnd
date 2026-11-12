package com.safezone.nodesecure.platform.sensors.domain.repositories;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.MacAddress;

import java.util.List;
import java.util.Optional;

public interface SensorNodeRepository {
    SensorNode save(SensorNode sensorNode);
    Optional<SensorNode> findByIdAndCompanyId(Long id, Long companyId);
    Optional<SensorNode> findByMacAddress(MacAddress macAddress);
    List<SensorNode> findAllByCompanyId(Long companyId);
    boolean existsByMacAddress(MacAddress macAddress);
    boolean existsByMacAddressAndIdIsNot(MacAddress macAddress, Long id);
    boolean existsByZoneId(Long zoneId);
    void deleteById(Long id);
}
