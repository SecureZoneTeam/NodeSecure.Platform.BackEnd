package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Data used by an administrator to add a team member")
public record CreateUserResource(
        @Schema(example = "mrojas")
        @NotBlank(message = "{iam.error.username.notBlank}")
        String username,

        @Schema(example = "maria.rojas@andina.pe")
        @NotBlank(message = "{iam.error.email.notBlank}")
        String email,

        @Schema(description = "Optional. When empty the default password is used", example = "12345678")
        String password,

        @Schema(example = "María Rojas Salas")
        @NotBlank(message = "{iam.error.fullName.notBlank}")
        String fullName,

        @Schema(example = "[\"ROLE_WAREHOUSE_KEEPER\"]")
        List<String> roles,

        @Schema(description = "Optional. When empty the company of the current user is used", example = "1")
        Long companyId,

        @Schema(example = "2")
        Long warehouseId) {
}
