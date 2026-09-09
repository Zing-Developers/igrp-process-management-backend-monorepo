package cv.igrp.framework.process.runtime.auth.core.adapter;

/**
 * The configured super-admin email and the one way every adapter compares against it: trimmed and
 * case-insensitive. An empty configuration means nobody is ever super admin.
 *
 * <p>It compares emails, never tokens. The {@code email} claim must come from a {@code Jwt} the
 * resource server already validated; this class deliberately has no way to parse a raw token.
 */
public final class SuperAdminEmail {

	private final String configured;

	public SuperAdminEmail(String configured) {
		this.configured = configured == null ? "" : configured.trim();
	}

	/** Whether {@code email} is the configured super admin (trimmed, case-insensitive). */
	public boolean matches(String email) {
		return !configured.isEmpty() && email != null && configured.equalsIgnoreCase(email.trim());
	}

}
