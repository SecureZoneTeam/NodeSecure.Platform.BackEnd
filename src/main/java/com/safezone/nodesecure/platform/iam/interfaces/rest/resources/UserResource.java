package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "User of the platform")
public record UserResource(
        @Schema(example = "1") Long id,
        @Schema(example = "fperez") String username,
        @Schema(example = "fernando.perez@safezone.pe") String email,
        @Schema(example = "Fernando Sebastián Pérez Bellido") String fullName,
        @Schema(example = "[\"ROLE_ADMINISTRATOR\"]") List<String> roles,
        @Schema(example = "1") Long companyId,
        @Schema(example = "4") Long warehouseId) {
}
