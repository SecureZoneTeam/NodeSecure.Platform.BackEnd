package com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa;

import com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.entities.SecurityAlertJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data detail, deliberately not a domain port.
 * Filtered listings use {@link JpaSpecificationExecutor} so every filter stays optional.
 */
public interface SecurityAlertJpaRepository
        extends JpaRepository<SecurityAlertJpaEntity, Long>, JpaSpecificationExecutor<SecurityAlertJpaEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SecurityAlertJpaEntity a where a.id = :id")
    Optional<SecurityAlertJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
