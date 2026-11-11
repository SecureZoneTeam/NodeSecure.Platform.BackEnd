package com.safezone.nodesecure.platform.shared.application.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void successKeepsValue() {
        Result<String, String> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("ok", result.success().orElseThrow());
        assertEquals("value: ok", result.fold(value -> "value: " + value, error -> "error: " + error));
    }

    @Test
    void failureKeepsError() {
        Result<String, String> result = Result.failure("bad");

        assertTrue(result.isFailure());
        assertTrue(result.success().isEmpty());
        assertEquals("bad", result.failure().orElseThrow());
        assertEquals("error: bad", result.fold(value -> "value: " + value, error -> "error: " + error));
    }
}
