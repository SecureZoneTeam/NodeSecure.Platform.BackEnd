package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.application.queries.GetAllUsersQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.CreateUserResource;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Team members and their roles")
public class UsersController {

    private static final String ADMIN_ONLY = "hasRole('ADMINISTRATOR')";

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

    @Operation(summary = "Get all users")
    @GetMapping
    public ResponseEntity<List<UserResource>> getAllUsers() {
        List<UserResource> users = queryService.handle(new GetAllUsersQuery()).stream()
                .map(UserResourceMapper::toResource)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get a user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return queryService.handle(new GetUserByIdQuery(id))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceMapper.toResource(user)))
                .orElseGet(() -> UserHttpResponseMapper.notFound(id, messages));
    }

    @Operation(summary = "Add a team member", description = "Only for administrators")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Not an administrator"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @PreAuthorize(ADMIN_ONLY)
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserResource resource) {
        var command = UserCommandMapper.toCreateCommand(resource, iamFacade.currentCompanyId());
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update a team member", description = "Only for administrators")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "403", description = "Not an administrator"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PreAuthorize(ADMIN_ONLY)
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserResource resource) {
        var command = UserCommandMapper.toUpdateCommand(id, resource);
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Remove a team member", description = "Only for administrators")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User removed"),
            @ApiResponse(responseCode = "403", description = "Not an administrator"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize(ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        var result = commandService.handle(new DeleteUserCommand(id));
        return UserHttpResponseMapper.toDeleteResponse(result, messages);
    }
}
