package cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the IRN API integration.
 */
@ConfigurationProperties(prefix = "irn.api")
public record IrnApiProperties(
        String baseUrl,
        String superAdminEmail,
        @DefaultValue("session_id") String sessionCookieName
) {
    /**
     * Returns the full URL for the /me endpoint.
     */
    public String getMeEndpoint() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/api/v1/Auth/me";
    }
}
