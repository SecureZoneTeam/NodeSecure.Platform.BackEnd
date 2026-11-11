package com.safezone.nodesecure.platform.iam.interfaces.rest.transform;

import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandFailure;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public class UserHttpResponseMapper {

    public static ResponseEntity<?> toResponse(Result<User, UserCommandFailure> result, HttpStatus okStatus,
                                               MessageSource messages) {
        return result.fold(
                user -> ResponseEntity.status(okStatus).body(UserResourceMapper.toResource(user)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toSignInResponse(Result<SignInResult, UserCommandFailure> result,
                                                     MessageSource messages) {
        return result.fold(
                signIn -> ResponseEntity.ok(UserResourceMapper.toAuthenticatedResource(signIn)),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<?> toDeleteResponse(Result<Long, UserCommandFailure> result,
                                                     MessageSource messages) {
        return result.fold(
                id -> ResponseEntity.noContent().build(),
                failure -> toProblem(failure, messages));
    }

    public static ResponseEntity<ProblemDetail> notFound(Long id, MessageSource messages) {
        return problem(HttpStatus.NOT_FOUND, "iam.error.notFoundById", messages, id);
    }

    private static ResponseEntity<ProblemDetail> toProblem(UserCommandFailure failure, MessageSource messages) {
        if (failure instanceof UserCommandFailure.NotFound notFound) {
            return notFound(notFound.id(), messages);
        }
        return problem(statusOf(failure), failure.messageKey(), messages);
    }

    private static HttpStatus statusOf(UserCommandFailure failure) {
        return switch (failure) {
            case UserCommandFailure.UsernameTaken taken -> HttpStatus.CONFLICT;
            case UserCommandFailure.EmailTaken taken -> HttpStatus.CONFLICT;
            case UserCommandFailure.InvalidCredentials invalid -> HttpStatus.UNAUTHORIZED;
            case UserCommandFailure.NotFound notFound -> HttpStatus.NOT_FOUND;
        };
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String key, MessageSource messages,
                                                         Object... args) {
        String detail = messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
