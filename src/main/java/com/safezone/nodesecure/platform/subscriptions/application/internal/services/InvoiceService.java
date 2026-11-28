package com.safezone.nodesecure.platform.subscriptions.application.internal.services;

import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Invoice;
import com.safezone.nodesecure.platform.subscriptions.domain.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** T28: invoices of the caller's organization (issued by {@link SubscriptionService}). */
@Service
public class InvoiceService {

    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Invoice> findAll(Long companyId, Long subscriptionId) {
        return subscriptionId == null
                ? repository.findAllByCompanyIdOrderByIssuedAtDescIdDesc(companyId)
                : repository.findAllByCompanyIdAndSubscriptionIdOrderByIssuedAtDescIdDesc(companyId, subscriptionId);
    }

    @Transactional(readOnly = true)
    public Invoice getById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("invoices.error.notFound", id));
    }
}
