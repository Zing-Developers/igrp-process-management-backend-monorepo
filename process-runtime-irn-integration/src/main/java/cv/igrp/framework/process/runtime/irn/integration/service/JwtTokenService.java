package cv.igrp.framework.process.runtime.irn.integration.service;

import cv.igrp.framework.process.runtime.irn.integration.config.security.JwtSigner;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating and caching JWT tokens.
 * Tokens are cached based on time windows to ensure automatic refresh before expiration.
 * This service is configured as a bean in RestClientSignedAuthorizationConfig.
 */
public class JwtTokenService {

    private final JwtSigner jwtSigner;
    private final String jwtKey;

    public JwtTokenService(JwtSigner jwtSigner, String jwtKey) {
        this.jwtSigner = jwtSigner;
        this.jwtKey = jwtKey;
    }

    /**
     * Generates a JWT token with 1-hour expiration.
     * The token is cached based on 55-minute time windows to ensure
     * automatic refresh 5 minutes before expiration.
     *
     * @return the generated JWT token
     */
    @Cacheable(value = "jwtTokens", key = "#root.target.getTimeWindow()")
    public String generateToken() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", jwtKey);

        // Add expiration to payload (1-hour TTL)
        long ttl = (System.currentTimeMillis() / 1000L) + 60 * 60;
        payload.put("exp", ttl);

        return jwtSigner.generateRS256Token(payload);
    }

    /**
     * Returns the current time window (55-minute intervals).
     * This ensures tokens are regenerated 5 minutes before they expire.
     *
     * @return the current time window identifier
     */
    public long getTimeWindow() {
        // 55 minutes in milliseconds = 55 * 60 * 1000 = 3,300,000
        return System.currentTimeMillis() / 3300000L;
    }
}
