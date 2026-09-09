package cv.igrp.framework.process.runtime.auth.irn.adapter;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IrnAuthorizationServiceAdapterTest {

	private static final IrnApiProperties PROPS =
			new IrnApiProperties("https://irn.test", "Admin@Irn.cv", "session_id");

	private final IrnAuthorizationCacheService cache = mock(IrnAuthorizationCacheService.class);
	private final IrnAuthorizationServiceAdapter adapter = new IrnAuthorizationServiceAdapter(cache, PROPS);

	private static String tokenWithEmail(String email) {
		var claims = new JWTClaimsSet.Builder().subject("u1");
		if (email != null) claims.claim("email", email);
		return new PlainJWT(claims.build()).serialize();
	}

	private static MockHttpServletRequest requestWithSession(String sessionId) {
		var request = new MockHttpServletRequest();
		if (sessionId != null) request.setCookies(new Cookie("session_id", sessionId));
		return request;
	}

	@Test
	void jwtEmailGrantsSuperAdminWithoutSessionCookie() {
		assertThat(adapter.isSuperAdmin(tokenWithEmail(" admin@irn.cv "), requestWithSession(null))).isTrue();
		verify(cache, never()).isSuperAdmin(any());
	}

	@Test
	void jwtEmailWinsEvenWhenSessionCookieIsPresent() {
		assertThat(adapter.isSuperAdmin(tokenWithEmail("ADMIN@IRN.CV"), requestWithSession("s1"))).isTrue();
		verify(cache, never()).isSuperAdmin(any());
	}

	@Test
	void otherEmailFallsThroughToIrnSession() {
		when(cache.isSuperAdmin("s1")).thenReturn(true);
		assertThat(adapter.isSuperAdmin(tokenWithEmail("other@irn.cv"), requestWithSession("s1"))).isTrue();
		verify(cache).isSuperAdmin("s1");
	}

	@Test
	void missingEmailClaimAndNoCookieIsNotSuperAdmin() {
		when(cache.isSuperAdmin(null)).thenReturn(false);
		assertThat(adapter.isSuperAdmin(tokenWithEmail(null), requestWithSession(null))).isFalse();
		verify(cache).isSuperAdmin(null);
	}

	@Test
	void garbageTokenFailsClosedToIrnSession() {
		when(cache.isSuperAdmin(null)).thenReturn(false);
		assertThat(adapter.isSuperAdmin("not-a-jwt", requestWithSession(null))).isFalse();
	}

	@Test
	void noConfiguredEmailNeverGrantsFromJwt() {
		var unconfigured = new IrnAuthorizationServiceAdapter(cache,
				new IrnApiProperties("https://irn.test", "", "session_id"));
		when(cache.isSuperAdmin("s1")).thenReturn(false);
		assertThat(unconfigured.isSuperAdmin(tokenWithEmail("admin@irn.cv"), requestWithSession("s1"))).isFalse();
		verify(cache).isSuperAdmin("s1");
	}

}
