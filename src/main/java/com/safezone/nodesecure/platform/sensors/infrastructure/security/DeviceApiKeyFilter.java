package com.safezone.nodesecure.platform.sensors.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Guards the public ESP32 ingestion route with a deployment-configured device API key. */
@Component
public class DeviceApiKeyFilter extends OncePerRequestFilter {

    private static final String EVENT_PATH = "/api/v1/physical-events";
    private static final String HEADER_NAME = "X-Device-Api-Key";

    private final byte[] expectedKey;

    public DeviceApiKeyFilter(@Value("${SENSOR_API_KEY:}") String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !EVENT_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER_NAME);
        if (expectedKey.length == 0 || provided == null
                || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"A valid device API key is required.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
