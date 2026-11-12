package com.safezone.nodesecure.platform.sensors.application.queryservices;

import com.safezone.nodesecure.platform.sensors.application.queries.GetAllSensorNodesQuery;
import com.safezone.nodesecure.platform.sensors.application.queries.GetSensorNodeByIdQuery;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;

import java.util.List;
import java.util.Optional;

public interface SensorQueryService {
    List<SensorNode> handle(GetAllSensorNodesQuery query);
    Optional<SensorNode> handle(GetSensorNodeByIdQuery query);
}
