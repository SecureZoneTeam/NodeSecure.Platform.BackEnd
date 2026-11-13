package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandFailure;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class StockMovementCommandHttpResponseMapper {

    public static ResponseEntity<?> toResponseEntityFromResult(
            Result<StockMovement, StockMovementCommandFailure> result,
            MessageSource messageSource) {
        return result.fold(
                movement -> new ResponseEntity<>(StockMovementResourceMapper.toResourceFromEntity(movement), HttpStatus.CREATED),
                failure -> InventoryProblemResponseMapper.problemDetail(statusFromFailure(failure), messageSource,
                        failure.messageKey(), failure.messageArgs()));
    }

    private static HttpStatus statusFromFailure(StockMovementCommandFailure failure) {
        if (failure instanceof StockMovementCommandFailure.ProductNotFound) {
            return HttpStatus.NOT_FOUND;
        }
        if (failure instanceof StockMovementCommandFailure.InsufficientStock) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
