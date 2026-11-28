package com.safezone.nodesecure.platform.subscriptions.domain.repositories;

import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByCompanyIdOrderByIdAsc(Long companyId);

    Optional<Subscription> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Subscription> findFirstByCompanyIdAndStatusOrderByIdDesc(Long companyId, String status);

    boolean existsByCompanyIdAndStatus(Long companyId, String status);

    /**
     * Locks the organization row until the end of the transaction, so two requests of the same
     * organization cannot activate or change subscriptions at the same time.
     */
    @Query(value = "SELECT id FROM organizations WHERE id = :companyId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockOrganization(@Param("companyId") Long companyId);
}
