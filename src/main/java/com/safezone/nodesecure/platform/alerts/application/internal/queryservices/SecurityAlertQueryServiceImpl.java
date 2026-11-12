package com.safezone.nodesecure.platform.alerts.application.internal.queryservices;

import com.safezone.nodesecure.platform.alerts.application.queries.GetAllSecurityAlertsQuery;
import com.safezone.nodesecure.platform.alerts.application.queries.GetSecurityAlertByIdQuery;
import com.safezone.nodesecure.platform.alerts.application.queryservices.SecurityAlertQueryService;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.repositories.SecurityAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SecurityAlertQueryServiceImpl implements SecurityAlertQueryService {
    private final SecurityAlertRepository repository;

    public SecurityAlertQueryServiceImpl(SecurityAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SecurityAlert> handle(GetAllSecurityAlertsQuery query) {
        return repository.findAll(query.status(), query.severity(), query.warehouseId());
    }

    @Override
    public Optional<SecurityAlert> handle(GetSecurityAlertByIdQuery query) {
        return repository.findById(query.id());
    }
}
