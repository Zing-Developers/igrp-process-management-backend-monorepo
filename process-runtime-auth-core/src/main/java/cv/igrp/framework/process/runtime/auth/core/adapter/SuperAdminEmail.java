package cv.igrp.framework.process.runtime.auth.core.adapter;

import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configured super-admin email and the one way every adapter compares against it: trimmed and
 * case-insensitive. An empty configuration means nobody is ever super admin.
 *
 * <p>{@link #matchesJwt(String)} reads the {@code email} claim from a token the resource server has
 * already validated, so no signature re-check happens here; an unparsable token fails closed.
 */
public final class SuperAdminEmail {

	private static final Logger log = LoggerFactory.getLogger(SuperAdminEmail.class);

	private final String configured;

	public SuperAdminEmail(String configured) {
		this.configured = configured == null ? "" : configured.trim();
	}

	public boolean isConfigured() {
		return !configured.isEmpty();
	}

	/** Whether {@code email} is the configured super admin (trimmed, case-insensitive). */
	public boolean matches(String email) {
		return isConfigured() && email != null && configured.equalsIgnoreCase(email.trim());
	}

	/** Whether the {@code email} claim of {@code jwt} is the configured super admin. */
	public boolean matchesJwt(String jwt) {
		if (!isConfigured() || jwt == null) {
			return false;
		}
		try {
			return matches(JWTParser.parse(jwt).getJWTClaimsSet().getStringClaim("email"));
		} catch (Exception e) {
			// fail closed: an unparsable token never grants privileges
			log.debug("Could not read email claim for super-admin check: {}", e.getMessage());
			return false;
		}
	}

}
