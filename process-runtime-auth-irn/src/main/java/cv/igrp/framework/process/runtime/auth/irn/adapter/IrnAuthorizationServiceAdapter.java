package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.IAuthorizationServiceAdapter;
import cv.igrp.framework.process.runtime.auth.core.adapter.SuperAdminEmail;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
	 * <p>The JWT is checked first: when its {@code email} claim matches, the user is super admin
	 * without any IRN session — the token was already validated by the resource server, and the
	 * super-admin role alone satisfies every route rule. Only when the claim does not match does the
	 * lookup fall through to the IRN {@code /Auth/me} response keyed by the session cookie, so an IRN
	 * user whose IdP email differs from the IRN one keeps working as before.
	 *
	 * @param jwt the JWT token, whose {@code email} claim is compared with the configured email
	 * @param request the HTTP request containing the session ID cookie (fallback)
	 * @return true if the user is a super admin, false otherwise
	 */
	@Override
	public boolean isSuperAdmin(String jwt, HttpServletRequest request) {
		if (superAdminEmail.matchesJwt(jwt)) {
			LOGGER.debug("Super admin granted from the JWT email claim; IRN session not consulted");
			return true;
		}
		String sessionId = extractSessionId(request);
		return cacheService.isSuperAdmin(sessionId);
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
		if (request == null || request.getCookies() == null) return null;

		return Arrays.stream(request.getCookies())
				.filter(c -> sessionCookieName.equals(c.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}
}
