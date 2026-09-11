package cv.igrp.framework.process.runtime.auth.core.adapter;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;


import java.util.Set;

public interface IAuthorizationServiceAdapter {

	Set<String> getGroups(String jwt, HttpServletRequest request);

	Set<String> getPermissions(String jwt, HttpServletRequest request);

	/**
	 * Preferred form: lets an adapter read the {@code email} claim of the validated token and grant the
	 * permissions the application mapped to it (see
	 * {@link cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver}) when the caller
	 * has no session. Defaults to the raw-token form for adapters that do not read claims.
	 */
	default Set<String> getPermissions(Jwt jwt, HttpServletRequest request) {
		return getPermissions(jwt.getTokenValue(), request);
	}

	/** Raw-token form. Adapters must not parse the string themselves; see the {@link Jwt} overload. */
	boolean isSuperAdmin(String jwt, HttpServletRequest request);

	/**
	 * Preferred form: the token as the resource server decoded and validated it, so an adapter can read
	 * claims (for example {@code email}) without re-parsing, and without ever trusting an unverified
	 * token. Defaults to the raw-token form for adapters that do not read claims.
	 */
	default boolean isSuperAdmin(Jwt jwt, HttpServletRequest request) {
		return isSuperAdmin(jwt.getTokenValue(), request);
	}

	Set<String> getActiveGroups(String jwt, HttpServletRequest request);

}
