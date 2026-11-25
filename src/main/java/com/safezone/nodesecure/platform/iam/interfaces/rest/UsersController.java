package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commands.ChangeUserStatusCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserPermissionsCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.application.queries.GetAllUsersQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.CreateUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateUserPermissionsResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserCommandMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserHttpResponseMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Team management ("Equipos y accesos"). Every operation is limited to the organization of the
 * signed-in user; users of other organizations answer 404.
 */
@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Workers of the organization, their roles, warehouses and permissions")
public class UsersController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public UsersController(UserCommandService commandService, UserQueryService queryService,
                           IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "Get the users of my organization")
    @PreAuthorize("hasAuthority('users.view')")
    @GetMapping
    public ResponseEntity<List<UserResource>> getAllUsers() {
        List<UserResource> users = queryService.handle(new GetAllUsersQuery(iamFacade.requireCompanyId())).stream()
                .map(UserResourceMapper::toResource)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get a user of my organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found in the organization")
    })
    @PreAuthorize("hasAuthority('users.view')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return queryService.handle(new GetUserByIdQuery(id, iamFacade.requireCompanyId()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceMapper.toResource(user)))
                .orElseGet(() -> UserHttpResponseMapper.notFound(id, messages));
    }

    @Operation(summary = "Create a worker account", description = "The account is active immediately. "
            + "The organization is the one of the signed-in owner; the password is stored hashed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or warehouses of another organization"),
            @ApiResponse(responseCode = "403", description = "Missing permission users.create"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @PreAuthorize("hasAuthority('users.create')")
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserResource resource) {
        var command = UserCommandMapper.toCreateCommand(resource, iamFacade.requireCompanyId(), iamFacade.currentUserId());
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update a worker", description = "Profile, role and assigned warehouses")
    @PreAuthorize("hasAuthority('users.update')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserResource resource) {
        var command = UserCommandMapper.toUpdateCommand(id, resource, iamFacade.requireCompanyId(),
                iamFacade.currentUserId());
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Replace the permission exceptions of a worker")
    @PreAuthorize("hasAuthority('users.update')")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<?> updatePermissions(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserPermissionsResource resource) {
        var command = new UpdateUserPermissionsCommand(iamFacade.requireCompanyId(), iamFacade.currentUserId(), id,
                resource.overrides());
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Suspend a worker", description = "Its sessions stop working immediately")
    @PreAuthorize("hasAuthority('users.suspend')")
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long id) {
        var command = new ChangeUserStatusCommand(iamFacade.requireCompanyId(), iamFacade.currentUserId(), id, true);
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Reactivate a worker")
    @PreAuthorize("hasAuthority('users.suspend')")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        var command = new ChangeUserStatusCommand(iamFacade.requireCompanyId(), iamFacade.currentUserId(), id, false);
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Remove a worker")
    @PreAuthorize("hasAuthority('users.update')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        var result = commandService.handle(new DeleteUserCommand(iamFacade.requireCompanyId(),
                iamFacade.currentUserId(), id));
        return UserHttpResponseMapper.toDeleteResponse(result, messages);
    }
}
