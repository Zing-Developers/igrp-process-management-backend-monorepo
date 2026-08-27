package cv.igrp.framework.process.runtime.auth.core.m2m;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * Routes each request to the right {@link AuthenticationManager} by bearer-token shape:
 * {@code Bearer igrpm2m_…} goes to the opaque M2M introspector, anything else to the JWT manager.
 *
 * <p>This is the resource server's native seam for two token types on one {@code Authorization}
 * header ({@code oauth2ResourceServer(o -> o.authenticationManagerResolver(...))}) — no custom
 * filter, no SecurityContext plumbing, and M2M failures surface through the standard bearer entry
 * point exactly like JWT failures.
 */
public final class M2mAuthenticationManagers {

	private static final String BEARER_M2M_PREFIX = "Bearer " + M2mKeyResolver.KEY_PREFIX;

	private M2mAuthenticationManagers() {
	}

	public static AuthenticationManagerResolver<HttpServletRequest> m2mAware(
			AuthenticationManager jwtAuthenticationManager,
			OpaqueTokenIntrospector m2mIntrospector) {

		final AuthenticationManager m2mManager =
				new ProviderManager(new OpaqueTokenAuthenticationProvider(m2mIntrospector));

		return request -> isM2m(request) ? m2mManager : jwtAuthenticationManager;
	}

	private static boolean isM2m(HttpServletRequest request) {
		final var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		return authorization != null && authorization.startsWith(BEARER_M2M_PREFIX);
	}

}
