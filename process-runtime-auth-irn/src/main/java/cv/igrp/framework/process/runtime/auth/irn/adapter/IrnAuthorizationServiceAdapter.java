package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.IAuthorizationServiceAdapter;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

	private final IrnAuthorizationCacheService cacheService;
	private final String sessionCookieName;

	public IrnAuthorizationServiceAdapter(IrnAuthorizationCacheService cacheService, IrnApiProperties properties) {
		this.cacheService = cacheService;
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
	 * Note: Permissions are currently disabled in this implementation and will always return an empty set.
	 *
	 * @param jwt the JWT token (not currently used in IRN implementation)
	 * @param request the HTTP request containing the session ID cookie
	 * @return empty set (permissions not currently enabled)
	 */
	@Override
	public Set<String> getPermissions(String jwt, HttpServletRequest request) {
		String sessionId = extractSessionId(request);
		return cacheService.getPermissions(sessionId);
	}

	/**
	 * Checks if the current user is a super admin.
	 * Super admin status is determined by comparing the user's email with the configured super admin email.
	 *
	 * @param jwt the JWT token (not currently used in IRN implementation)
	 * @param request the HTTP request containing the session ID cookie
	 * @return true if the user is a super admin, false otherwise
	 */
	@Override
	public boolean isSuperAdmin(String jwt, HttpServletRequest request) {
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
		if (request.getCookies() == null) return null;

		return Arrays.stream(request.getCookies())
				.filter(c -> sessionCookieName.equals(c.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}
}
