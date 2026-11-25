package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.application.queries.GetOrganizationByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignInResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignUpResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserCommandMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserHttpResponseMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Sign in, owner sign up and current session")
public class AuthenticationController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public AuthenticationController(UserCommandService commandService, UserQueryService queryService,
                                    IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "Sign in", description = "Validates the credentials and returns a JWT with the session context")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed in"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Suspended account or organization")
    })
    @SecurityRequirements
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        var result = commandService.handle(UserCommandMapper.toSignInCommand(resource));
        return UserHttpResponseMapper.toSignInResponse(result, HttpStatus.OK, messages);
    }

    @Operation(summary = "Sign up", description = "Creates a new organization and its owner (ROLE_OWNER). "
            + "Workers are created by the owner from /api/v1/users, never through this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization and owner created; the session is returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @SecurityRequirements
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        var result = commandService.handle(UserCommandMapper.toSignUpCommand(resource));
        return UserHttpResponseMapper.toSignInResponse(result, HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Current session", description = "Role, permissions, warehouses and organization of the signed-in user")
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        SessionUser session = iamFacade.currentSession().orElse(null);
        if (session == null) {
            return UserHttpResponseMapper.problem(HttpStatus.UNAUTHORIZED, "iam.error.invalidCredentials", messages);
        }
        var organization = queryService.handle(new GetOrganizationByIdQuery(session.companyId())).orElse(null);
        return queryService.handle(new GetUserByIdQuery(session.id(), session.companyId()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        UserResourceMapper.toAuthenticatedResource(user, organization, null)))
                .orElseGet(() -> UserHttpResponseMapper.notFound(session.id(), messages));
    }
}
