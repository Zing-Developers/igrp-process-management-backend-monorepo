package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.IrnAuthClient;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data.IrnMeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Caches the IRN {@code /Auth/me} lookup per session.
 *
 * <p>Groups, permissions and super-admin status all come from the same response, so without this the
 * same request would call IRN three times. It lives in its own bean because {@code @Cacheable} only
 * applies through the Spring proxy, and self-invocation bypasses it.
 */
@Service
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "irn"
)
public class IrnMeCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(IrnMeCache.class);

	private final IrnAuthClient client;

	public IrnMeCache(IrnAuthClient client) {
		this.client = client;
	}

	/**
	 * Returns the current user as reported by IRN, or {@code null} when the session is missing or the
	 * lookup fails. Failed lookups are not cached, so a transient IRN outage does not lock the caller
	 * out for the whole cache TTL.
	 *
	 * @param sessionId the IRN session id taken from the request cookie
	 * @return the response, or {@code null} if it could not be retrieved
	 */
	@Cacheable(value = "irnMeCache", key = "#sessionId", unless = "#result == null")
	public IrnMeResponse me(String sessionId) {

		if (sessionId == null || sessionId.isBlank()) {
			LOGGER.debug("No IRN session id on the request; cannot resolve the current user");
			return null;
		}

		try {
			return client.getMe(sessionId);
		} catch (Exception e) {
			LOGGER.error("SECURITY: failed to resolve the current user from IRN", e);
			return null;
		}
	}

}
