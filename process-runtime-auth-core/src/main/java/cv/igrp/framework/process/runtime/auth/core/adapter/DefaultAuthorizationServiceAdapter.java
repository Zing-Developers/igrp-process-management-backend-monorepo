package cv.igrp.framework.process.runtime.auth.core.adapter;

import cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "default",
		matchIfMissing = true
)
public class DefaultAuthorizationServiceAdapter implements IAuthorizationServiceAdapter {

	private static final Logger log = LoggerFactory.getLogger(DefaultAuthorizationServiceAdapter.class);

	/**
	 * Optional super-admin escape for the provider-less default mode, mirroring the IRN adapter's
	 * {@code irn.api.super-admin-email}: when set, a validated JWT whose {@code email} claim matches
	 * (trimmed, case-insensitive) is treated as super admin. Empty (the default) keeps the old
	 * behaviour, nobody is ever super admin.
	 */
	private final SuperAdminEmail superAdminEmail;
	private final EmailAccessResolver emailAccess;

	public DefaultAuthorizationServiceAdapter(
			@Value("${igrp.authorization.default.super-admin-email:}") String superAdminEmail,
			EmailAccessResolver emailAccess) {
		this.superAdminEmail = new SuperAdminEmail(superAdminEmail);
		this.emailAccess = emailAccess;
	}


	@Override
	public Set<String> getGroups(String jwt, HttpServletRequest request) {
		log.debug("Fetching groups for user: {}", jwt);
		return Set.of();
	}

	@Override
	public Set<String> getPermissions(String jwt, HttpServletRequest request) {
		log.debug("Fetching permissions for user: {}", jwt);
		return Set.of();
	}

	/**
	 * There is no session in the default mode, so the validated token's {@code email} claim is the only
	 * identity: whatever the application mapped to it (format-checked) is granted.
	 */
	@Override
	public Set<String> getPermissions(Jwt jwt, HttpServletRequest request) {
		return EmailAccess.permissionsFor(emailAccess, jwt);
	}

	/** Raw tokens are never parsed here: without the decoded {@link Jwt} nobody is super admin. */
	@Override
	public boolean isSuperAdmin(String jwt, HttpServletRequest request) {
		return false;
	}

	@Override
	public boolean isSuperAdmin(Jwt jwt, HttpServletRequest request) {
		return superAdminEmail.matches(jwt.getClaimAsString("email"));
	}

	@Override
	public Set<String> getActiveGroups(String jwt, HttpServletRequest request) {
		log.debug("Fetching active groups for user: {}", jwt);
		return Set.of();
	}

}
