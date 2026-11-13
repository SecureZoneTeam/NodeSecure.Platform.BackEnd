package com.safezone.nodesecure.platform.reporting.domain.repositories;

import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
}
