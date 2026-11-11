package com.safezone.nodesecure.platform.iam.interfaces.rest;

import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignInResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignUpResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserCommandMapper;
import com.safezone.nodesecure.platform.iam.interfaces.rest.transform.UserHttpResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Sign in and sign up")
@SecurityRequirements
public class AuthenticationController {

    private final UserCommandService commandService;
    private final MessageSource messages;

    public AuthenticationController(UserCommandService commandService, MessageSource messages) {
        this.commandService = commandService;
        this.messages = messages;
    }

    @Operation(summary = "Sign in", description = "Validates the credentials and returns a JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed in"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        var result = commandService.handle(UserCommandMapper.toSignInCommand(resource));
        return UserHttpResponseMapper.toSignInResponse(result, messages);
    }

    @Operation(summary = "Sign up", description = "Creates a new account with the warehouse keeper role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        var result = commandService.handle(UserCommandMapper.toSignUpCommand(resource));
        return UserHttpResponseMapper.toResponse(result, HttpStatus.CREATED, messages);
    }
}
