package cv.igrp.framework.process.runtime.auth.core.m2m;

import java.util.Optional;

/**
 * SPI: resolves a raw machine-to-machine API key to the client it identifies.
 *
 * <p>Implementations hash the raw key (HMAC-SHA-256 with a server-side pepper) and look it up in
 * their credential store, honouring revocation ({@code active=false}) and expiry. They must
 * <strong>fail closed</strong>: any store failure is an empty result or an exception — never a
 * fallthrough that authenticates.
 *
 * <p>A no-op default (empty result for every key) is auto-configured so applications can wire the
 * M2M-aware authentication path unconditionally; without a real implementation every M2M key is
 * simply rejected.
 */
public interface M2mKeyResolver {

	/** Keys are {@code igrpm2m_<32 random bytes base64url>}; the prefix makes leaked keys scannable. */
	String KEY_PREFIX = "igrpm2m_";

	/**
	 * @param rawKey the presented key, prefix included
	 * @return the resolved client, or empty when the key is unknown, revoked or expired
	 */
	Optional<M2mKey> resolve(String rawKey);

}
