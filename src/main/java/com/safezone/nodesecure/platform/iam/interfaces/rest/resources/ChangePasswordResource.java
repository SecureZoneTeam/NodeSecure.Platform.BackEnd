package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Current password (confirmation) and the new one")
public record ChangePasswordResource(
        @NotBlank(message = "{iam.error.password.notBlank}") String currentPassword,
        @Schema(description = "8 to 72 characters")
        @NotBlank(message = "{iam.error.password.notBlank}") String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordResource[****]";
    }
}
