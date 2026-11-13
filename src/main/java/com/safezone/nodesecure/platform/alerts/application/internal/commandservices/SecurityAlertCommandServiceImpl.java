package com.safezone.nodesecure.platform.alerts.application.internal.commandservices;

import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandFailure;
import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandService;
import com.safezone.nodesecure.platform.alerts.application.commands.MarkAlertAsFalseAlarmCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.RaiseSecurityAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.ReconcileAlertCommand;
import com.safezone.nodesecure.platform.alerts.domain.exceptions.AlertAlreadyResolvedException;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.repositories.SecurityAlertRepository;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.function.Consumer;

@Service
public class SecurityAlertCommandServiceImpl implements SecurityAlertCommandService {
    private final SecurityAlertRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public SecurityAlertCommandServiceImpl(SecurityAlertRepository repository,
                                           ApplicationEventPublisher eventPublisher,
                                           Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<SecurityAlert, SecurityAlertCommandFailure> handle(RaiseSecurityAlertCommand command) {
        var alert = SecurityAlert.raise(command.warehouseId(), command.zoneId(), command.physicalEventId(),
                command.alertType(), command.severity(), command.message(), clock.instant());
        // Events are pulled before saving: the saved instance is rebuilt from the database and carries none.
        var events = alert.pullDomainEvents();
        var saved = repository.save(alert);
        events.forEach(eventPublisher::publishEvent);
        return Result.success(saved);
    }

    @Override
    @Transactional
    public Result<SecurityAlert, SecurityAlertCommandFailure> handle(MarkAlertAsFalseAlarmCommand command) {
        return resolve(command.alertId(), alert -> alert.markAsFalseAlarm(command.justification(), command.resolvedBy(), clock.instant()));
    }

    @Override
    @Transactional
    public Result<SecurityAlert, SecurityAlertCommandFailure> handle(ReconcileAlertCommand command) {
        return resolve(command.alertId(), alert -> alert.reconcile(command.justification(), command.resolvedBy(), clock.instant()));
    }

    private Result<SecurityAlert, SecurityAlertCommandFailure> resolve(Long alertId, Consumer<SecurityAlert> closeAction) {
        // The alert row is locked so two people cannot close the same alert at the same time.
        var found = repository.findByIdForUpdate(alertId);
        if (found.isEmpty())
            return Result.failure(new SecurityAlertCommandFailure.NotFound(alertId));
        var alert = found.get();
        try {
            closeAction.accept(alert);
        } catch (AlertAlreadyResolvedException e) {
            return Result.failure(new SecurityAlertCommandFailure.AlreadyResolved(e.getAlertId(), e.getStatus()));
        }
        var events = alert.pullDomainEvents();
        var saved = repository.save(alert);
        events.forEach(eventPublisher::publishEvent);
        return Result.success(saved);
    }
}
