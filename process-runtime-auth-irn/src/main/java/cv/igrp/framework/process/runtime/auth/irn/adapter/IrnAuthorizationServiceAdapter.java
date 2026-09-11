package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.IAuthorizationServiceAdapter;
import cv.igrp.framework.process.runtime.auth.core.adapter.SuperAdminEmail;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * IRN-specific implementation of the authorization service adapter.
 * Handles authentication and authorization by communicating with the IRN API.
 * Delegates to IrnAuthorizationCacheService for cached operations to ensure proper Spring AOP proxy behavior.
 */
@Component
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "irn"
)
public class IrnAuthorizationServiceAdapter implements IAuthorizationServiceAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(IrnAuthorizationServiceAdapter.class);

	private final IrnAuthorizationCacheService cacheService;
	private final SuperAdminEmail superAdminEmail;
	private final String sessionCookieName;

	public IrnAuthorizationServiceAdapter(IrnAuthorizationCacheService cacheService, IrnApiProperties properties) {
		this.cacheService = cacheService;
		this.superAdminEmail = new SuperAdminEmail(properties.superAdminEmail());
		this.sessionCookieName = properties.sessionCookieName();
	}



	/**
	 * Retrieves the roles/space information for the current user from the IRN API.
	 * Results are cached based on the session ID.
	 *
	 * @param jwt the JWT token (not currently used in IRN implementation)
	 * @param request the HTTP request containing the session ID cookie
	 * @return set of role/space identifiers, or empty set if roles cannot be retrieved
	 */
	@Override
	public Set<String> getGroups(String jwt, HttpServletRequest request) {
		String sessionId = extractSessionId(request);
        return cacheService.getGroups(sessionId);
	}

	/**
	 * Retrieves permissions for the current user from the IRN API.
	 *
	 * @param jwt the JWT token (not currently used in IRN implementation)
	 * @param request the HTTP request containing the session ID cookie
	 * @return the user's IRN permissions, or empty set if they cannot be retrieved
	 */
	@Override
	public Set<String> getPermissions(String jwt, HttpServletRequest request) {
		String sessionId = extractSessionId(request);
		return cacheService.getPermissions(sessionId);
	}

	/**
	 * Checks if the current user is the configured super admin ({@code irn.api.super-admin-email}).
	 *
	 * <p>When the request carries an IRN session cookie, the IRN {@code /Auth/me} email decides, as it
	 * always did: IRN stays the authority for users it knows, and revoking them there still removes the
	 * role within the cache TTL. Only a request with no session cookie (a super admin calling with a
	 * bare token) falls back to the {@code email} claim of the validated JWT.
	 *
	 * @param jwt the token as decoded and validated by the resource server
	 * @param request the HTTP request that may carry the session ID cookie
	 * @return true if the user is a super admin, false otherwise
	 */
	@Override
	public boolean isSuperAdmin(Jwt jwt, HttpServletRequest request) {
		String sessionId = extractSessionId(request);
		if (sessionId != null && !sessionId.isBlank()) {
			return cacheService.isSuperAdmin(sessionId);
		}
		final var granted = superAdminEmail.matches(jwt.getClaimAsString("email"));
		if (granted) {
			LOGGER.debug("Super admin granted from the validated JWT email claim; no IRN session on the request");
		}
		return granted;
	}

	/**
	 * Raw-token form: session only. The string is never parsed here, so a caller that has not been
	 * through the resource server cannot obtain the role from a claim.
	 *
	 * @param jwt the raw token (unused)
	 * @param request the HTTP request containing the session ID cookie
	 * @return true if the IRN session belongs to the super admin, false otherwise
	 */
	@Override
	public boolean isSuperAdmin(String jwt, HttpServletRequest request) {
		return cacheService.isSuperAdmin(extractSessionId(request));
	}
	/**
	 * Retrieves the active roles for the current user.
	 * In the IRN implementation, active roles are the same as regular roles.
	 *
	 * @param jwt the JWT token (not currently used in IRN implementation)
	 * @param request the HTTP request containing the session ID cookie
	 * @return set of active role/spaces identifiers (same as getRoles)
	 */
	@Override
	public Set<String> getActiveGroups(String jwt, HttpServletRequest request) {
		// Active roles/spaces are the same as regular roles in IRN implementation
		return getGroups(jwt, request);
	}

	/**
	 * Extracts the session ID from the request cookies.
	 *
	 * @param request the HTTP request
	 * @return the session ID, or null if not found
	 */
	private String extractSessionId(HttpServletRequest request) {
		if (request.getCookies() == null) return null;

		return Arrays.stream(request.getCookies())
				.filter(c -> sessionCookieName.equals(c.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}
}
