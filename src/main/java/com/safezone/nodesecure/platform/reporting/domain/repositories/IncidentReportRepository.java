package com.safezone.nodesecure.platform.reporting.domain.repositories;

import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    List<IncidentReport> findAllByCompanyIdOrderByIdAsc(Long companyId);

    Optional<IncidentReport> findByIdAndCompanyId(Long id, Long companyId);

    Optional<IncidentReport> findByCompanyIdAndWarehouseIdAndPeriodStartAndPeriodEnd(Long companyId, Long warehouseId,
                                                                                      Instant periodStart, Instant periodEnd);
}
