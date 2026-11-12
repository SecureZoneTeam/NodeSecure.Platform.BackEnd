package com.safezone.nodesecure.platform.sensors.interfaces.rest.transform;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.sensors.application.commandservices.SensorCommandFailure;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class SensorHttpResponseMapper {
    private SensorHttpResponseMapper() { }

    public static ResponseEntity<?> toSensorResponse(Result<SensorNode, SensorCommandFailure> result,
                                                     HttpStatus successStatus, MessageSource messages) {
        return result.fold(node -> ResponseEntity.status(successStatus).body(SensorResourceMapper.toResource(node)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toEventResponse(Result<PhysicalEvent, SensorCommandFailure> result,
                                                     HttpStatus successStatus, MessageSource messages) {
        return result.fold(event -> ResponseEntity.status(successStatus).body(SensorResourceMapper.toResource(event)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toDeleteResponse(Result<Long, SensorCommandFailure> result, MessageSource messages) {
        return result.fold(id -> ResponseEntity.noContent().build(), failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<ProblemDetail> notFound(String resource, Long id, MessageSource messages) {
        return problem(HttpStatus.NOT_FOUND, "sensor.error.notFound", messages, resource, id);
    }

    private static ResponseEntity<ProblemDetail> toProblem(SensorCommandFailure failure, MessageSource messages) {
        if (failure instanceof SensorCommandFailure.NotFound notFound) {
            return notFound(notFound.resource(), notFound.id(), messages);
        }
        if (failure instanceof SensorCommandFailure.UnregisteredMacAddress unknownMac) {
            return problem(HttpStatus.NOT_FOUND, unknownMac.messageKey(), messages, unknownMac.macAddress());
        }
        HttpStatus status = failure instanceof SensorCommandFailure.Conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return problem(status, failure.messageKey(), messages);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String key, MessageSource messages, Object... args) {
        String detail = messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
