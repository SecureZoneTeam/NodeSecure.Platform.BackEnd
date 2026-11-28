package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.subscriptions.domain.exceptions.PlanLimitExceededException;
import com.safezone.nodesecure.platform.subscriptions.domain.exceptions.SubscriptionRequiredException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.LimitViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structured 409 answers for plan limits, so the client can tell the user which resource is
 * affected, how much is used, what the plan allows and offer a plan change. Runs before the
 * generic {@code GlobalExceptionHandler}, which would only return the message.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SubscriptionExceptionHandler {

    private final MessageSource messageSource;

    public SubscriptionExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    ProblemDetail handle(PlanLimitExceededException exception, Locale locale) {
        ProblemDetail problem = problem(exception.getMessage(), exception.getArguments(), "Plan limit", locale);
        problem.setProperty("code", exception.getCode());
        problem.setProperty("planCode", exception.getPlanCode());
        problem.setProperty("planName", exception.getPlanName());
        List<Map<String, Object>> violations = exception.getViolations().stream().map(SubscriptionExceptionHandler::toMap).toList();
        problem.setProperty("violations", violations);
        if (!violations.isEmpty()) {
            violations.getFirst().forEach(problem::setProperty);
        }
        log.warn("Plan limit: {} {}", exception.getCode(), exception.getViolations());
        return problem;
    }

    @ExceptionHandler(SubscriptionRequiredException.class)
    ProblemDetail handle(SubscriptionRequiredException exception, Locale locale) {
        ProblemDetail problem = problem(exception.getMessage(), null, "Subscription required", locale);
        problem.setProperty("code", SubscriptionRequiredException.CODE);
        problem.setProperty("resource", exception.getResource().key());
        return problem;
    }

    private ProblemDetail problem(String key, Object[] arguments, String title, Locale locale) {
        String detail = messageSource.getMessage(key, arguments, key, locale);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle(title);
        return problem;
    }

    private static Map<String, Object> toMap(LimitViolation violation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("resource", violation.resource().key());
        map.put("used", violation.used());
        map.put("limit", violation.limit());
        return map;
    }
}
