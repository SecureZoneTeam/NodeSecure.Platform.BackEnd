package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commands.UpdateOrganizationCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.OrganizationCommandService;
import com.safezone.nodesecure.platform.iam.application.queries.GetOrganizationByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateOrganizationResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserHttpResponseMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** Only "current" is exposed: there is no way to address another organization by id. */
@RestController
@RequestMapping(value = "/api/v1/organizations", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Organizations", description = "Organization (tenant) of the signed-in user")
public class OrganizationsController {

    private final UserQueryService queryService;
    private final OrganizationCommandService commandService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public OrganizationsController(UserQueryService queryService, OrganizationCommandService commandService,
                                   IamContextFacade iamFacade, MessageSource messages) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "My organization")
    @PreAuthorize("hasAuthority('settings.view')")
    @GetMapping("/current")
    public ResponseEntity<?> getCurrent() {
        return queryService.handle(new GetOrganizationByIdQuery(iamFacade.requireCompanyId()))
                .<ResponseEntity<?>>map(organization -> ResponseEntity.ok(UserResourceMapper.toResource(organization)))
                .orElseGet(() -> UserHttpResponseMapper.problem(HttpStatus.NOT_FOUND,
                        "iam.error.organization.unavailable", messages));
    }

    @Operation(summary = "Rename my organization")
    @PreAuthorize("hasAuthority('settings.manage')")
    @PatchMapping("/current")
    public ResponseEntity<?> updateCurrent(@Valid @RequestBody UpdateOrganizationResource resource) {
        return commandService.handle(new UpdateOrganizationCommand(iamFacade.requireCompanyId(), resource.name()))
                .<ResponseEntity<?>>map(organization -> ResponseEntity.ok(UserResourceMapper.toResource(organization)))
                .orElseGet(() -> UserHttpResponseMapper.problem(HttpStatus.NOT_FOUND,
                        "iam.error.organization.unavailable", messages));
    }
}
