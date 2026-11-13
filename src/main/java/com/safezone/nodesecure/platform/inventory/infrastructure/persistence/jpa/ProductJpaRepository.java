package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa;

import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities.ProductJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    boolean existsByCompanyIdAndSku(Long companyId, String sku);

    boolean existsByCompanyIdAndSkuAndIdNot(Long companyId, String sku, Long id);

    List<ProductJpaEntity> findAllByOrderByIdAsc();

    List<ProductJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductJpaEntity p where p.id = :id")
    Optional<ProductJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
