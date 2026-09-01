package cv.igrp.framework.process.runtime.auth.core.adapter;

import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
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
	 * {@code irn.api.super-admin-email}: when set, a JWT whose {@code email} claim matches (trimmed,
	 * case-insensitive) is treated as super admin. Empty (the default) keeps today's behaviour —
	 * nobody is ever super admin. The claim is read from the already-validated token; no signature
	 * re-check is needed here because the resource server verified it before authorities are built.
	 */
	private final String superAdminEmail;

	public DefaultAuthorizationServiceAdapter(
			@Value("${igrp.authorization.default.super-admin-email:}") String superAdminEmail) {
		this.superAdminEmail = superAdminEmail == null ? "" : superAdminEmail.trim();
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

	@Override
	public boolean isSuperAdmin(String jwt, HttpServletRequest request) {
		if (superAdminEmail.isEmpty()) {
			return false;
		}
		try {
			final var email = JWTParser.parse(jwt).getJWTClaimsSet().getStringClaim("email");
			return email != null && superAdminEmail.equalsIgnoreCase(email.trim());
		} catch (Exception e) {
			// fail closed: an unparsable token never grants privileges
			log.debug("Could not read email claim for super-admin check: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public Set<String> getActiveGroups(String jwt, HttpServletRequest request) {
		log.debug("Fetching active groups for user: {}", jwt);
		return Set.of();
	}

}
