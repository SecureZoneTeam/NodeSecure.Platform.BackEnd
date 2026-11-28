package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fields of my own profile to change. Null or blank fields keep their current value")
public record UpdateProfileResource(
        @Schema(example = "Juan Quispe Mamani") String fullName,
        @Schema(example = "juan.quispe@andina.pe") String email,
        @Schema(example = "jquispe") String username) {
}
