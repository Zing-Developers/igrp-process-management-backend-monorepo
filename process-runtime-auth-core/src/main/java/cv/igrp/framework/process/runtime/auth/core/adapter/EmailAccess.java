package cv.igrp.framework.process.runtime.auth.core.adapter;

import cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;
import java.util.Set;

/**
 * The one way adapters turn a validated token into mapped permissions: read the {@code email} claim,
 * normalise it (trim, lower-case), ask the application's {@link EmailAccessResolver}, keep only
 * well-formed {@code MODULE:action} entries. No claim, no lookup.
 */
public final class EmailAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailAccess.class);

	private EmailAccess() {
	}

	public static Set<String> permissionsFor(EmailAccessResolver resolver, Jwt jwt) {
		final var email = jwt == null ? null : jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			return Set.of();
		}
		final var granted = PermissionFormat.onlyValid(resolver.resolve(email.trim().toLowerCase(Locale.ROOT)));
		if (!granted.isEmpty()) {
			LOGGER.debug("Granted {} permissions from the email access mapping; no IRN session on the request", granted.size());
		}
		return granted;
	}

}
