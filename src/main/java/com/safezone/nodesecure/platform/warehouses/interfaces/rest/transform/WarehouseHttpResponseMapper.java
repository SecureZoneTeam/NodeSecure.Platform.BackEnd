package com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.warehouses.application.commandservices.WarehouseCommandFailure;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class WarehouseHttpResponseMapper {
    private WarehouseHttpResponseMapper() { }

    public static ResponseEntity<?> toWarehouseResponse(Result<Warehouse, WarehouseCommandFailure> result,
                                                         HttpStatus successStatus, MessageSource messages) {
        return result.fold(warehouse -> ResponseEntity.status(successStatus).body(WarehouseResourceMapper.toResource(warehouse)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toZoneResponse(Result<WarehouseZone, WarehouseCommandFailure> result,
                                                    HttpStatus successStatus, MessageSource messages) {
        return result.fold(zone -> ResponseEntity.status(successStatus).body(WarehouseResourceMapper.toZoneResource(zone)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toDeleteResponse(Result<Long, WarehouseCommandFailure> result, MessageSource messages) {
        return result.fold(id -> ResponseEntity.noContent().build(), failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<ProblemDetail> notFound(String resource, Long id, MessageSource messages) {
        return problem(HttpStatus.NOT_FOUND, "warehouse.error.notFound", messages, resource, id);
    }

    private static ResponseEntity<ProblemDetail> toProblem(WarehouseCommandFailure failure, MessageSource messages) {
        if (failure instanceof WarehouseCommandFailure.NotFound notFound) {
            return notFound(notFound.resource(), notFound.id(), messages);
        }
        HttpStatus status = failure instanceof WarehouseCommandFailure.Conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return problem(status, failure.messageKey(), messages);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String key, MessageSource messages, Object... args) {
        String detail = messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
