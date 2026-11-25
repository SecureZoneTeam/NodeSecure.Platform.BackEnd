package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Organization (tenant) of the signed-in user")
public record OrganizationResource(
        @Schema(example = "1") Long id,
        @Schema(example = "Distribuidora Andina S.A.C.") String name,
        @Schema(example = "1") Long ownerUserId,
        @Schema(example = "ACTIVE") String status,
        Instant createdAt) {
}
