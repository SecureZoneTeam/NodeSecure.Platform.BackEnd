package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Fields to change on a user. Empty fields keep their current value")
public record UpdateUserResource(
        @Schema(example = "maria.rojas@andina.pe")
        String email,

        @Schema(example = "María Rojas Salas")
        String fullName,

        @Schema(example = "[\"ROLE_SECURITY_MANAGER\"]")
        List<String> roles,

        @Schema(example = "3")
        Long warehouseId) {
}
