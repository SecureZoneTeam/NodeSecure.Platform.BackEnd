package com.safezone.nodesecure.platform.subscriptions.domain.repositories;

import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByCompanyId(Long companyId);

    Optional<Company> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Company> findByTaxIdAndCompanyId(String taxId, Long companyId);

    boolean existsByCompanyId(Long companyId);

    boolean existsByTaxId(String taxId);
}
