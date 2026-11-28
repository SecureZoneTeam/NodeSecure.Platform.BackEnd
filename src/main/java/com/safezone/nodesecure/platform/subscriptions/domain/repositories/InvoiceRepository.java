package com.safezone.nodesecure.platform.subscriptions.domain.repositories;

import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Invoice;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Invoices are only issued and read: there is no update or delete operation. */
@org.springframework.stereotype.Repository
public interface InvoiceRepository extends Repository<Invoice, Long> {

    Invoice save(Invoice invoice);

    List<Invoice> findAllByCompanyIdOrderByIssuedAtDescIdDesc(Long companyId);

    List<Invoice> findAllByCompanyIdAndSubscriptionIdOrderByIssuedAtDescIdDesc(Long companyId, Long subscriptionId);

    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsBySubscriptionIdAndPeriodStart(Long subscriptionId, Instant periodStart);
}
