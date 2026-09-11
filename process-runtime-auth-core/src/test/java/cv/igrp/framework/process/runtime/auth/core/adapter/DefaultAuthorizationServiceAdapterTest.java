package cv.igrp.framework.process.runtime.auth.core.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthorizationServiceAdapterTest {

	/** A decoded token as the resource server hands it over; the claim, not the encoding, is what matters. */
	static Jwt tokenWithEmail(String email) {
		var jwt = Jwt.withTokenValue("validated-by-the-resource-server").header("alg", "RS256").subject("u1");
		if (email != null) jwt.claim("email", email);
		return jwt.build();
	}

	@Test
	void nobodyIsSuperAdminWhenNoEmailIsConfigured() {
		var adapter = new DefaultAuthorizationServiceAdapter("");
		assertThat(adapter.isSuperAdmin(tokenWithEmail("admin@x.cv"), null)).isFalse();
	}

	@Test
	void matchingEmailClaimGrantsSuperAdminCaseInsensitively() {
		var adapter = new DefaultAuthorizationServiceAdapter("Admin@X.cv");
		assertThat(adapter.isSuperAdmin(tokenWithEmail("admin@x.cv "), null)).isTrue();
		assertThat(adapter.isSuperAdmin(tokenWithEmail("other@x.cv"), null)).isFalse();
		assertThat(adapter.isSuperAdmin(tokenWithEmail(null), null)).isFalse();
	}

	@Test
	void rawTokenStringsNeverGrant() {
		// the escape only reads a token the resource server decoded; a bare string is never parsed
		var adapter = new DefaultAuthorizationServiceAdapter("admin@x.cv");
		assertThat(adapter.isSuperAdmin("eyJhbGciOiJub25lIn0.eyJlbWFpbCI6ImFkbWluQHguY3YifQ.", null)).isFalse();
		assertThat(adapter.isSuperAdmin("not-a-jwt", null)).isFalse();
	}

}
