package cv.igrp.framework.process.runtime.auth.core.m2m;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class M2mOpaqueTokenIntrospectorTest {

	private static final String KEY = "igrpm2m_abcdef";

	@Test
	void resolvedKeyBecomesAnM2mPrincipalWithItsPermissionsPlusBaseAuthorities() {

		var introspector = new M2mOpaqueTokenIntrospector(
				key -> Optional.of(new M2mKey("fila-job", Set.of("TASK_INSTANCES:visualizar"))),
				List.of("ROLE_ACTIVITI_USER"));

		var principal = introspector.introspect(KEY);

		assertThat(principal.getName()).isEqualTo("m2m:fila-job");
		assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("TASK_INSTANCES:visualizar", "ROLE_ACTIVITI_USER");
	}

	@Test
	void unknownKeyIsRejected() {

		var introspector = new M2mOpaqueTokenIntrospector(key -> Optional.empty(), List.of());

		assertThatThrownBy(() -> introspector.introspect(KEY))
				.isInstanceOf(BadOpaqueTokenException.class);
	}

	@Test
	void storeFailureFailsClosedInsteadOfFallingThrough() {

		var introspector = new M2mOpaqueTokenIntrospector(
				key -> { throw new IllegalStateException("db down"); }, List.of());

		assertThatThrownBy(() -> introspector.introspect(KEY))
				.isInstanceOf(BadOpaqueTokenException.class);
	}

	@Test
	void rolesAndMalformedStringsCannotSmuggleThroughThePermissionsColumn() {

		// A permissions value of ROLE_DEPT_IGRP.superadmin would be a skeleton key: SecurityConfig
		// appends that role to every route rule. The format gate drops anything that is not MODULE:action.
		var introspector = new M2mOpaqueTokenIntrospector(
				key -> Optional.of(new M2mKey("evil", Set.of(
						"ROLE_DEPT_IGRP.superadmin", "GROUP_X", "task:View", "TASK_INSTANCES:visualizar"))),
				List.of());

		var principal = introspector.introspect(KEY);

		assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("TASK_INSTANCES:visualizar");
	}

}
