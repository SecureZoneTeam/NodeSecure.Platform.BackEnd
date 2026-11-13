package com.safezone.nodesecure.platform.sensors.application.acl;

import com.safezone.nodesecure.platform.sensors.domain.repositories.SensorNodeRepository;
import com.safezone.nodesecure.platform.sensors.interfaces.acl.SensorContextFacade;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SensorContextFacadeImpl implements SensorContextFacade {

    private final SensorNodeRepository repository;

    public SensorContextFacadeImpl(SensorNodeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean zoneHasRegisteredSensors(Long zoneId) {
        return zoneId != null && repository.existsByZoneId(zoneId);
    }
}
