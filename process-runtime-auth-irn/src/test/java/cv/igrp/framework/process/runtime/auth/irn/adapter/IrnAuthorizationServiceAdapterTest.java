package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IrnAuthorizationServiceAdapterTest {

	private static final IrnApiProperties PROPS =
			new IrnApiProperties("https://irn.test", "Admin@Irn.cv", "session_id");

	private final IrnAuthorizationCacheService cache = mock(IrnAuthorizationCacheService.class);
	private final EmailAccessResolver mapping = mock(EmailAccessResolver.class);
	private final IrnAuthorizationServiceAdapter adapter = new IrnAuthorizationServiceAdapter(cache, PROPS, mapping);

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
				new IrnApiProperties("https://irn.test", "", "session_id"), mapping);
		assertThat(unconfigured.isSuperAdmin(tokenWithEmail("admin@irn.cv"), requestWithSession(null))).isFalse();
		verify(cache, never()).isSuperAdmin(any());
	}

	// --- permissions: session first, email access mapping only without a session ---

	@Test
	void withoutSessionCookieTheMappedPermissionsAreGrantedFormatChecked() {
		when(mapping.resolve("svc@x.cv")).thenReturn(Set.of("TASK_INSTANCES:visualizar", "ROLE_DEPT_IGRP.superadmin", "ROLE_X:y"));

		assertThat(adapter.getPermissions(tokenWithEmail(" Svc@X.cv "), requestWithSession(null)))
				.containsExactly("TASK_INSTANCES:visualizar");
		// a blank cookie value counts as no session, like isSuperAdmin
		assertThat(adapter.getPermissions(tokenWithEmail("svc@x.cv"), requestWithSession("  ")))
				.containsExactly("TASK_INSTANCES:visualizar");
		verify(cache, never()).getPermissions(any());
	}

	@Test
	void withSessionCookieIrnDecidesAndTheMappingIsNeverConsulted() {
		when(cache.getPermissions("s1")).thenReturn(Set.of("FILA_TRABALHO:visualizar"));
		assertThat(adapter.getPermissions(tokenWithEmail("svc@x.cv"), requestWithSession("s1")))
				.containsExactly("FILA_TRABALHO:visualizar");

		// IRN failure (cache returns empty) stays a denial: no fallback to the mapping
		when(cache.getPermissions("s1")).thenReturn(Set.of());
		assertThat(adapter.getPermissions(tokenWithEmail("svc@x.cv"), requestWithSession("s1"))).isEmpty();
		verify(mapping, never()).resolve(any());
	}

	@Test
	void noEmailClaimAndNoCookieGrantsNothingWithoutALookup() {
		assertThat(adapter.getPermissions(tokenWithEmail(null), requestWithSession(null))).isEmpty();
		verify(mapping, never()).resolve(any());
	}

	@Test
	void mappingNeverGrantsGroupsAndTheRawTokenFormStaysSessionOnly() {
		when(mapping.resolve("svc@x.cv")).thenReturn(Set.of("TASK_INSTANCES:visualizar"));
		when(cache.getGroups(null)).thenReturn(Set.of());
		when(cache.getPermissions(null)).thenReturn(Set.of());

		assertThat(adapter.getActiveGroups("raw", requestWithSession(null))).isEmpty();
		assertThat(adapter.getPermissions("raw", requestWithSession(null))).isEmpty();
		verify(mapping, never()).resolve(any());
	}

	@Test
	void mappingStoreFailurePropagates() {
		when(mapping.resolve("svc@x.cv")).thenThrow(new IllegalStateException("db down"));
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> adapter.getPermissions(tokenWithEmail("svc@x.cv"), requestWithSession(null)))
				.isInstanceOf(IllegalStateException.class);
	}

}
