package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data used to create a new account")
public record SignUpResource(
        @Schema(example = "jquispe")
        @NotBlank(message = "{iam.error.username.notBlank}")
        String username,

        @Schema(example = "juan.quispe@andina.pe")
        @NotBlank(message = "{iam.error.email.notBlank}")
        String email,

        @Schema(example = "12345678")
        @NotBlank(message = "{iam.error.password.notBlank}")
        @Size(min = 8, max = 72, message = "{iam.error.password.size}")
        String password,

        @Schema(example = "Juan Quispe Mamani")
        @NotBlank(message = "{iam.error.fullName.notBlank}")
        String fullName,

        @Schema(example = "1")
        Long companyId) {
}
