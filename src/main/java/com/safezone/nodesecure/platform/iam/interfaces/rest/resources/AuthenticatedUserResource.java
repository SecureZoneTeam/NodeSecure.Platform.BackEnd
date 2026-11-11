package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Signed in user with its JWT token")
public record AuthenticatedUserResource(
        @Schema(example = "1") Long id,
        @Schema(example = "fperez") String username,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "[\"ROLE_ADMINISTRATOR\"]") List<String> roles,
        @Schema(example = "4") Long warehouseId) {
}
