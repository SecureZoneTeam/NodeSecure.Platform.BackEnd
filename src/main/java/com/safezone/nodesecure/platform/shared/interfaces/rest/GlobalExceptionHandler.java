package com.safezone.nodesecure.platform.shared.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleException(MethodArgumentNotValidException exception, Locale locale) {
        String prefix = messageSource.getMessage("errors.found", null, locale);
        String fields = exception.getFieldErrors().stream()
                .map(fieldError -> messageSource.getMessage(fieldError, locale))
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", fields);
        return ErrorResponse.create(
                exception,
                HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()),
                prefix + " " + fields
        );
    }


    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleException(IllegalArgumentException exception, Locale locale) {
        String messageKey = exception.getMessage() != null ? exception.getMessage() : "errors.found";
        String message = Objects.requireNonNullElse(messageSource.getMessage(messageKey, null, messageKey, locale), messageKey);
        log.warn("Illegal argument: {}", message);
        return ErrorResponse.create(exception, HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), message);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleException(HttpMessageNotReadableException exception, Locale locale) {
        String message = messageSource.getMessage("errors.malformedBody", null, "errors.malformedBody", locale);
        log.warn("Malformed request body: {}", exception.getMessage());
        return ErrorResponse.create(exception, HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleException(MethodArgumentTypeMismatchException exception, Locale locale) {
        String message = messageSource.getMessage("errors.invalidParameter", new Object[]{exception.getName()},
                "errors.invalidParameter", locale);
        log.warn("Invalid parameter {}", exception.getName());
        return ErrorResponse.create(exception, HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), message);
    }

}
