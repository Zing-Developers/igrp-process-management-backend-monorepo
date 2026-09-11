package cv.igrp.framework.process.runtime.auth.core.adapter;

import cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultAuthorizationServiceAdapterTest {

	private static final EmailAccessResolver NO_MAPPING = email -> Set.of();

	/** A decoded token as the resource server hands it over; the claim, not the encoding, is what matters. */
	static Jwt tokenWithEmail(String email) {
		var jwt = Jwt.withTokenValue("validated-by-the-resource-server").header("alg", "RS256").subject("u1");
		if (email != null) jwt.claim("email", email);
		return jwt.build();
	}

	@Test
	void nobodyIsSuperAdminWhenNoEmailIsConfigured() {
		var adapter = new DefaultAuthorizationServiceAdapter("", NO_MAPPING);
		assertThat(adapter.isSuperAdmin(tokenWithEmail("admin@x.cv"), null)).isFalse();
	}

	@Test
	void matchingEmailClaimGrantsSuperAdminCaseInsensitively() {
		var adapter = new DefaultAuthorizationServiceAdapter("Admin@X.cv", NO_MAPPING);
		assertThat(adapter.isSuperAdmin(tokenWithEmail("admin@x.cv "), null)).isTrue();
		assertThat(adapter.isSuperAdmin(tokenWithEmail("other@x.cv"), null)).isFalse();
		assertThat(adapter.isSuperAdmin(tokenWithEmail(null), null)).isFalse();
	}

	@Test
	void rawTokenStringsNeverGrant() {
		// the escape only reads a token the resource server decoded; a bare string is never parsed
		var adapter = new DefaultAuthorizationServiceAdapter("admin@x.cv", NO_MAPPING);
		assertThat(adapter.isSuperAdmin("eyJhbGciOiJub25lIn0.eyJlbWFpbCI6ImFkbWluQHguY3YifQ.", null)).isFalse();
		assertThat(adapter.isSuperAdmin("not-a-jwt", null)).isFalse();
		assertThat(adapter.getPermissions("not-a-jwt", null)).isEmpty();
	}

	@Test
	void mappedEmailGrantsItsWellFormedPermissionsOnly() {
		var resolver = mock(EmailAccessResolver.class);
		when(resolver.resolve("svc@x.cv")).thenReturn(Set.of("TASK_INSTANCES:visualizar", "ROLE_DEPT_IGRP.superadmin", "ROLE_X:y"));
		var adapter = new DefaultAuthorizationServiceAdapter("", resolver);

		// normalised before the lookup: trimmed and lower-cased
		assertThat(adapter.getPermissions(tokenWithEmail(" Svc@X.cv "), null)).containsExactly("TASK_INSTANCES:visualizar");
		assertThat(adapter.getActiveGroups("t", null)).isEmpty();
	}

	@Test
	void noEmailClaimMeansNoLookup() {
		var resolver = mock(EmailAccessResolver.class);
		var adapter = new DefaultAuthorizationServiceAdapter("", resolver);
		assertThat(adapter.getPermissions(tokenWithEmail(null), null)).isEmpty();
		assertThat(adapter.getPermissions(tokenWithEmail("   "), null)).isEmpty();
		verify(resolver, never()).resolve(any());
	}

	@Test
	void storeFailurePropagatesSoTheConverterFailsClosed() {
		var adapter = new DefaultAuthorizationServiceAdapter("", email -> { throw new IllegalStateException("db down"); });
		assertThatThrownBy(() -> adapter.getPermissions(tokenWithEmail("svc@x.cv"), null))
				.isInstanceOf(IllegalStateException.class);
	}

}
