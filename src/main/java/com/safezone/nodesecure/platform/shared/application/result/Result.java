package com.safezone.nodesecure.platform.shared.application.result;

import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }

    default Optional<T> success() {
        if (this instanceof Success<T, E> success) {
            return Optional.ofNullable(success.value());
        }
        return Optional.empty();
    }

    default Optional<E> failure() {
        if (this instanceof Failure<T, E> failure) {
            return Optional.of(failure.error());
        }
        return Optional.empty();
    }

    default <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure) {
        if (this instanceof Success<T, E> success) {
            return onSuccess.apply(success.value());
        }
        Failure<T, E> failure = (Failure<T, E>) this;
        return onFailure.apply(failure.error());
    }

    record Success<T, E>(T value) implements Result<T, E> {
    }

    record Failure<T, E>(E error) implements Result<T, E> {
    }
}
