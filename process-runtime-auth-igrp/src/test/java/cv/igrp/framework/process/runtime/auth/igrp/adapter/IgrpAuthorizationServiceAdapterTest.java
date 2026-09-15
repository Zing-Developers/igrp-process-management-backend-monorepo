package cv.igrp.framework.process.runtime.auth.igrp.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class IgrpAuthorizationServiceAdapterTest {

	@Test
	void jwtOverloadsReadTheDecodedTokenWithoutTheDecoder() {
		var decoder = mock(JwtDecoder.class);
		var adapter = new IgrpAuthorizationServiceAdapter(decoder);
		var jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("u1")
				.claim("permissions", List.of("AREAS:visualizar")).claim("is_super_admin", true).build();

		assertThat(adapter.getPermissions(jwt, null)).containsExactly("AREAS:visualizar");
		assertThat(adapter.isSuperAdmin(jwt, null)).isTrue();
		verifyNoInteractions(decoder);
	}

}
