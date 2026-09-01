package cv.igrp.framework.process.runtime.auth.core.adapter;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthorizationServiceAdapterTest {

	private static String tokenWithEmail(String email) {
		var claims = new JWTClaimsSet.Builder().subject("u1");
		if (email != null) claims.claim("email", email);
		return new PlainJWT(claims.build()).serialize();
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
	void garbageTokensFailClosed() {
		var adapter = new DefaultAuthorizationServiceAdapter("admin@x.cv");
		assertThat(adapter.isSuperAdmin("not-a-jwt", null)).isFalse();
	}

}
