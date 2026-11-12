package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.repositories;

import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities.SensorNodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorNodeJpaRepository extends JpaRepository<SensorNodeJpaEntity, Long> {
    Optional<SensorNodeJpaEntity> findByIdAndCompanyId(Long id, Long companyId);
    Optional<SensorNodeJpaEntity> findByMacAddress(String macAddress);
    List<SensorNodeJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);
    boolean existsByMacAddress(String macAddress);
    boolean existsByMacAddressAndIdIsNot(String macAddress, Long id);
    boolean existsByZoneId(Long zoneId);
}
