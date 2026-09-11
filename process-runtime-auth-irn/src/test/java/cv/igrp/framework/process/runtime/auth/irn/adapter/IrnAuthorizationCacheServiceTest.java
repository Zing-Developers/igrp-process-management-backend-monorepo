package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data.IrnMeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IrnAuthorizationCacheServiceTest {

	private final IrnMeCache meCache = mock(IrnMeCache.class);
	private final IrnAuthorizationCacheService service = new IrnAuthorizationCacheService(meCache,
			new IrnApiProperties("https://irn.test", "Admin@Irn.cv", "session_id"));

	private static IrnMeResponse me(String email) {
		return new IrnMeResponse("1", email, "Admin", true, List.of(), List.of(), null, null, null, null);
	}

	@Test
	void irnEmailIsMatchedTrimmedAndCaseInsensitively() {
		when(meCache.me("s1")).thenReturn(me(" admin@irn.cv "));
		assertThat(service.isSuperAdmin("s1")).isTrue();
	}

	@Test
	void otherIrnEmailIsNotSuperAdmin() {
		when(meCache.me("s1")).thenReturn(me("other@irn.cv"));
		assertThat(service.isSuperAdmin("s1")).isFalse();
	}

	@Test
	void missingSessionNeverReachesTheCache() {
		assertThat(service.isSuperAdmin(null)).isFalse();
		assertThat(service.isSuperAdmin(" ")).isFalse();
		assertThat(service.getGroups(null)).isEmpty();
		assertThat(service.getPermissions(null)).isEmpty();
		verify(meCache, never()).me(any());
	}

}
