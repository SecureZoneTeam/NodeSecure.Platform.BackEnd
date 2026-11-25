package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Worker account created directly by the owner. The organization is taken from the session;
 * any organization or company id sent by the client is ignored.
 */
@Schema(description = "Data used by the owner to create a worker account")
public record CreateUserResource(
        @Schema(example = "María Rojas Salas")
        @NotBlank(message = "{iam.error.fullName.notBlank}")
        @Size(max = 120, message = "{iam.error.fullName.size}")
        String fullName,
        @Schema(example = "maria.rojas@andina.pe")
        @NotBlank(message = "{iam.error.email.notBlank}")
        String email,
        @Schema(example = "mrojas")
        @NotBlank(message = "{iam.error.username.notBlank}")
        String username,
        @Schema(example = "Str0ngPass!")
        @NotBlank(message = "{iam.error.password.notBlank}")
        @Size(min = 8, max = 72, message = "{iam.error.password.size}")
        String password,
        @Schema(example = "ROLE_OPERATOR", allowableValues = {"ROLE_SUPERVISOR", "ROLE_OPERATOR"})
        @NotBlank(message = "{iam.error.role.invalid}")
        String role,
        @Schema(description = "Assigned warehouses (at least one)", example = "[1]")
        List<Long> warehouseIds,
        @Schema(description = "Single assigned warehouse; used when warehouseIds is empty", example = "1")
        Long warehouseId) {
}
