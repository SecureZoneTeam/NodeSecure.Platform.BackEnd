package com.safezone.nodesecure.platform.sensors.application.internal.queryservices;

import com.safezone.nodesecure.platform.sensors.application.queries.GetAllSensorNodesQuery;
import com.safezone.nodesecure.platform.sensors.application.queries.GetSensorNodeByIdQuery;
import com.safezone.nodesecure.platform.sensors.application.queryservices.SensorQueryService;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.ConnectionStatus;
import com.safezone.nodesecure.platform.sensors.domain.repositories.SensorNodeRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SensorQueryServiceImpl implements SensorQueryService {

    private final SensorNodeRepository repository;
    private final WarehouseContextFacade warehouseFacade;
    private final Clock clock;
    private final Duration connectionTimeout;

    public SensorQueryServiceImpl(SensorNodeRepository repository, WarehouseContextFacade warehouseFacade,
                                  Clock clock,
                                  @Value("${SENSOR_CONNECTION_TIMEOUT_SECONDS:120}") long timeoutSeconds) {
        this.repository = repository;
        this.warehouseFacade = warehouseFacade;
        this.clock = clock;
        this.connectionTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    @Override
    @Transactional
    public List<SensorNode> handle(GetAllSensorNodesQuery query) {
        Instant now = clock.instant();
        Set<Long> assignedZoneIds = query.warehouseId() == null
                ? null
                : warehouseFacade.findZoneIdsByWarehouseId(query.warehouseId(), query.companyId());
        return repository.findAllByCompanyId(query.companyId()).stream()
                .filter(node -> assignedZoneIds == null || assignedZoneIds.contains(node.getZoneId()))
                .map(node -> refreshStatus(node, now))
                .toList();
    }

    @Override
    @Transactional
    public Optional<SensorNode> handle(GetSensorNodeByIdQuery query) {
        return repository.findByIdAndCompanyId(query.id(), query.companyId())
                .filter(node -> belongsToWarehouse(node, query))
                .map(node -> refreshStatus(node, clock.instant()));
    }

    private boolean belongsToWarehouse(SensorNode node, GetSensorNodeByIdQuery query) {
        if (query.warehouseId() == null) return true;
        return warehouseFacade.findZoneConfiguration(node.getZoneId(), query.companyId())
                .map(configuration -> configuration.warehouseId().equals(query.warehouseId()))
                .orElse(false);
    }

    private SensorNode refreshStatus(SensorNode node, Instant now) {
        if (node.getConnectionStatus() == ConnectionStatus.ONLINE && node.connectionExpired(now, connectionTimeout)) {
            node.markOffline(now);
            return repository.save(node);
        }
        return node;
    }
}
