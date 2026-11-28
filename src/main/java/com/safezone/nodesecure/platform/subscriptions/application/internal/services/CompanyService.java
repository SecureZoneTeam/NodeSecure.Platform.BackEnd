package com.safezone.nodesecure.platform.subscriptions.application.internal.services;

import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Company;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.TaxId;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** T25: registration and lookup of the legal data (RUC) of the caller's organization. */
@Service
public class CompanyService {

    private final CompanyRepository repository;
    private final Clock clock;

    public CompanyService(CompanyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record RegisterCompany(String legalName, String taxId, String contactEmail, String country) {
    }

    @Transactional
    public Company register(Long companyId, RegisterCompany command) {
        TaxId taxId = new TaxId(command.taxId());
        if (repository.existsByCompanyId(companyId)) {
            throw new BusinessRuleViolationException("companies.error.alreadyRegistered");
        }
        if (repository.existsByTaxId(taxId.value())) {
            throw new BusinessRuleViolationException("companies.error.taxId.duplicated", taxId.value());
        }
        // The unique keys on tax_id and company_id still protect against concurrent requests (409).
        return repository.save(Company.register(companyId, command.legalName(), taxId, command.contactEmail(),
                command.country(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public Optional<Company> findMine(Long companyId) {
        return repository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Company getById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("companies.error.notFound", id));
    }

    /** Search by RUC inside the caller's organization: it never reveals companies of other organizations. */
    @Transactional(readOnly = true)
    public Optional<Company> findByTaxId(String taxId, Long companyId) {
        return repository.findByTaxIdAndCompanyId(new TaxId(taxId).value(), companyId);
    }
}
