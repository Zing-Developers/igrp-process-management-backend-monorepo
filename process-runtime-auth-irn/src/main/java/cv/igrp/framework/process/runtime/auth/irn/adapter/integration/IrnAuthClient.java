package cv.igrp.framework.process.runtime.auth.irn.adapter.integration;

import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data.IrnMeResponse;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.exception.IrnAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Client for interacting with the IRN authentication API.
 */
@Service
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "irn"
)
public class IrnAuthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IrnAuthClient.class);
    private final RestClient restClient;
    private final IrnApiProperties properties;

    public IrnAuthClient(RestClient restClient, IrnApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Retrieves the current user information from the IRN API.
     *
     * @param sessionId the session ID from the user's cookie
     * @return the user information response
     * @throws IrnAuthException if the API call fails or returns an error
     */
    public IrnMeResponse getMe(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IrnAuthException("Session ID cannot be null or empty");
        }

        try {
            return restClient.get()
                    .uri(properties.getMeEndpoint())
                    .header("Cookie", properties.sessionCookieName() + "=" + sessionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        LOGGER.error("Client error from IRN API: {} - {}", response.getStatusCode(), response.getStatusText());
                        throw new IrnAuthException("Authentication failed: " + response.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        LOGGER.error("Server error from IRN API: {} - {}", response.getStatusCode(), response.getStatusText());
                        throw new IrnAuthException("IRN API server error: " + response.getStatusCode());
                    })
                    .body(IrnMeResponse.class);
        } catch (IrnAuthException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error calling IRN API", e);
            throw new IrnAuthException("Failed to retrieve user information from IRN API", e);
        }
    }
}
