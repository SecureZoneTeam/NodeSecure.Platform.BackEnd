package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationResource(
        @Schema(example = "Distribuidora Andina S.A.C.")
        @NotBlank(message = "{iam.error.organization.name.notBlank}")
        @Size(max = 120, message = "{iam.error.organization.name.size}")
        String name) {
}
