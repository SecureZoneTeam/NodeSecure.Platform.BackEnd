package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials used to sign in")
public record SignInResource(
        @Schema(example = "fperez")
        @NotBlank(message = "{iam.error.username.notBlank}")
        String username,

        @Schema(example = "12345678")
        @NotBlank(message = "{iam.error.password.notBlank}")
        String password) {
}
