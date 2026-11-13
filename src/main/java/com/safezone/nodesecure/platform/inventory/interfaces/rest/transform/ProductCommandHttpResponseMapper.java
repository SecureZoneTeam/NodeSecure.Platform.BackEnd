package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commandservices.ProductCommandFailure;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.interfaces.rest.transform.ProblemResponseMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ProductCommandHttpResponseMapper {

    public static ResponseEntity<?> toResponseEntityFromResult(
            Result<Product, ProductCommandFailure> result,
            MessageSource messageSource,
            HttpStatus successStatus) {
        return result.fold(
                product -> new ResponseEntity<>(ProductResourceMapper.toResourceFromEntity(product), successStatus),
                failure -> ProblemResponseMapper.problemDetail(statusFromFailure(failure), messageSource,
                        failure.messageKey(), failure.messageArgs()));
    }

    private static HttpStatus statusFromFailure(ProductCommandFailure failure) {
        if (failure instanceof ProductCommandFailure.NotFound) {
            return HttpStatus.NOT_FOUND;
        }
        if (failure instanceof ProductCommandFailure.DuplicateSku) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
