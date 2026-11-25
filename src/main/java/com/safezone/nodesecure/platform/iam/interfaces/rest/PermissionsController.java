package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.PermissionResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/permissions", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Permissions", description = "Permission catalog and role defaults")
public class PermissionsController {

    @Operation(summary = "Permission catalog", description = "Every permission, whether it is owner-only and the roles that have it by default")
    @PreAuthorize("hasAuthority('users.view')")
    @GetMapping
    public ResponseEntity<List<PermissionResource>> getCatalog() {
        return ResponseEntity.ok(UserResourceMapper.toPermissionCatalog());
    }
}
