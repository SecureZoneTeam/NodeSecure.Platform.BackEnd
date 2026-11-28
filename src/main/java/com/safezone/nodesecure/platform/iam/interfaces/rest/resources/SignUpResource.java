package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Schema(description = "Data used to register a new owner and its organization")
public record SignUpResource(
        @Schema(example = "Juan Quispe Mamani")
        @NotBlank(message = "{iam.error.fullName.notBlank}")
        @Size(max = 120, message = "{iam.error.fullName.size}")
        String fullName,
        @Schema(example = "juan.quispe@andina.pe")
        @NotBlank(message = "{iam.error.email.notBlank}")
        String email,
        @Schema(example = "jquispe")
        @NotBlank(message = "{iam.error.username.notBlank}")
        String username,
        @Schema(example = "Str0ngPass!")
        @NotBlank(message = "{iam.error.password.notBlank}")
        @Size(min = 8, max = 72, message = "{iam.error.password.size}")
        String password,
        @Schema(example = "Distribuidora Andina S.A.C.")
        @NotBlank(message = "{iam.error.organization.name.notBlank}")
        @Size(max = 120, message = "{iam.error.organization.name.size}")
        String organizationName) {
}
