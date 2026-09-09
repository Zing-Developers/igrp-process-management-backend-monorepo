package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IrnAuthorizationServiceAdapterTest {

	private static final IrnApiProperties PROPS =
			new IrnApiProperties("https://irn.test", "Admin@Irn.cv", "session_id");

	private final IrnAuthorizationCacheService cache = mock(IrnAuthorizationCacheService.class);
	private final IrnAuthorizationServiceAdapter adapter = new IrnAuthorizationServiceAdapter(cache, PROPS);

	/** A decoded token as the resource server hands it over. */
	private static Jwt tokenWithEmail(String email) {
		var jwt = Jwt.withTokenValue("validated").header("alg", "RS256").subject("u1");
		if (email != null) jwt.claim("email", email);
		return jwt.build();
	}

	private static MockHttpServletRequest requestWithSession(String sessionId) {
		var request = new MockHttpServletRequest();
		if (sessionId != null) request.setCookies(new Cookie("session_id", sessionId));
		return request;
	}

	@Test
	void withoutSessionCookieTheValidatedEmailClaimDecides() {
		assertThat(adapter.isSuperAdmin(tokenWithEmail(" admin@irn.cv "), requestWithSession(null))).isTrue();
		assertThat(adapter.isSuperAdmin(tokenWithEmail("other@irn.cv"), requestWithSession(null))).isFalse();
		verify(cache, never()).isSuperAdmin(any());
	}

	@Test
	void withSessionCookieIrnDecidesEvenWhenTheEmailMatches() {
		// IRN stays the authority for users it knows: a revoked/expired session denies the matching email
		when(cache.isSuperAdmin("s1")).thenReturn(false);
		assertThat(adapter.isSuperAdmin(tokenWithEmail("ADMIN@IRN.CV"), requestWithSession("s1"))).isFalse();
		when(cache.isSuperAdmin("s1")).thenReturn(true);
		assertThat(adapter.isSuperAdmin(tokenWithEmail("other@irn.cv"), requestWithSession("s1"))).isTrue();
		verify(cache, times(2)).isSuperAdmin("s1");
	}

	@Test
	void missingEmailClaimAndNoCookieIsNotSuperAdmin() {
		assertThat(adapter.isSuperAdmin(tokenWithEmail(null), requestWithSession(null))).isFalse();
		verify(cache, never()).isSuperAdmin(any());
	}

	@Test
	void rawTokenFormIsSessionOnly() {
		when(cache.isSuperAdmin(null)).thenReturn(false);
		assertThat(adapter.isSuperAdmin("eyJhbGciOiJub25lIn0.eyJlbWFpbCI6ImFkbWluQGlybi5jdiJ9.", requestWithSession(null))).isFalse();
		verify(cache).isSuperAdmin(null);
	}

	@Test
	void noConfiguredEmailNeverGrantsFromJwt() {
		var unconfigured = new IrnAuthorizationServiceAdapter(cache,
				new IrnApiProperties("https://irn.test", "", "session_id"));
		assertThat(unconfigured.isSuperAdmin(tokenWithEmail("admin@irn.cv"), requestWithSession(null))).isFalse();
		verify(cache, never()).isSuperAdmin(any());
	}

}
