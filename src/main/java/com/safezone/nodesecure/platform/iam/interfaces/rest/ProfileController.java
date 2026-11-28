package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commands.ChangeOwnPasswordCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateOwnProfileCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.ChangePasswordResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateProfileResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserHttpResponseMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Profile of the signed-in user (Settings). The user is always the one of the session: there is
 * no id in the path, so nobody can edit another account here. Passwords are never returned.
 */
@RestController
@RequestMapping(value = "/api/v1/profile", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Profile", description = "My own account: full name, email, username and password")
public class ProfileController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public ProfileController(UserCommandService commandService, UserQueryService queryService,
                             IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "My profile")
    @GetMapping
    public ResponseEntity<?> getProfile() {
        SessionUser session = session();
        return queryService.handle(new GetUserByIdQuery(session.id(), session.companyId()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceMapper.toResource(user)))
                .orElseGet(() -> UserHttpResponseMapper.notFound(session.id(), messages));
    }

    @Operation(summary = "Update my full name, email or username")
    @ApiResponse(responseCode = "409", description = "Email or username already registered")
    @PatchMapping
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileResource resource) {
        SessionUser session = session();
        var command = new UpdateOwnProfileCommand(session.id(), session.companyId(), resource.fullName(),
                resource.email(), resource.username());
        return UserHttpResponseMapper.toResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Change my password",
            description = "Requires the current password. Every token issued before stops working; the response carries the new session token.")
    @ApiResponse(responseCode = "400", description = "Current password incorrect or new password invalid")
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordResource resource) {
        SessionUser session = session();
        var command = new ChangeOwnPasswordCommand(session.id(), session.companyId(), resource.currentPassword(),
                resource.newPassword());
        return UserHttpResponseMapper.toSignInResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    private SessionUser session() {
        return iamFacade.currentSession().orElseThrow(() -> new AccessDeniedException("No session"));
    }
}
