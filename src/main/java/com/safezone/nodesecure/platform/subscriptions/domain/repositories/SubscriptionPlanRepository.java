package com.safezone.nodesecure.platform.subscriptions.domain.repositories;

import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findAllByActiveTrueOrderByMonthlyPriceAsc();

    Optional<SubscriptionPlan> findByIdAndActiveTrue(Long id);

    Optional<SubscriptionPlan> findByCodeIgnoreCaseAndActiveTrue(String code);

    Optional<SubscriptionPlan> findByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByCodeIgnoreCase(String code);
}
