package cv.igrp.framework.process.runtime.auth.igrp.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.IAuthorizationServiceAdapter;
import cv.igrp.framework.process.runtime.auth.igrp.constants.Claim;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "igrp"
)
public class IgrpAuthorizationServiceAdapter
		implements IAuthorizationServiceAdapter {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(IgrpAuthorizationServiceAdapter.class);

	private final JwtDecoder jwtDecoder;

	public IgrpAuthorizationServiceAdapter(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	private Jwt decode(String token) {

		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException(
					"JWT token cannot be null or blank"
			);
		}

		return jwtDecoder.decode(token);
	}

	private Set<String> getStringSetClaim(Jwt token, Claim claim) {

		Object value = token.getClaim(claim.value());

		if (value instanceof Collection<?> values) {

			return values.stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.collect(Collectors.toUnmodifiableSet());
		}

		if (value instanceof String stringValue
				&& !stringValue.isBlank()) {

			return Set.of(stringValue);
		}

		return Set.of();
	}

	private Stream<String> getStringClaim(Jwt token, Claim claim) {

		String value = token.getClaimAsString(claim.value());

		return value == null || value.isBlank()
				? Stream.empty()
				: Stream.of(value);
	}

	@Override
	@Cacheable(
			value = "authorization-groups-cache",
			key = "#jwt",
			unless = "#result.isEmpty()"
	)
	public Set<String> getGroups(
			String jwt,
			HttpServletRequest request
	) {

		try {

			LOGGER.debug("Extracting groups from JWT");

			Jwt token = decode(jwt);

			return getStringSetClaim(token, Claim.ROLES);

		} catch (Exception e) {

			LOGGER.error("Error extracting groups from JWT", e);

			return Set.of();
		}
	}

	@Override
	@Cacheable(
			value = "authorization-active-groups-cache",
			key = "#jwt",
			unless = "#result.isEmpty()"
	)
	public Set<String> getActiveGroups(
			String jwt,
			HttpServletRequest request
	) {

		try {

			LOGGER.debug("Extracting active groups from JWT");

			Jwt token = decode(jwt);

			return Stream.concat(
							getStringClaim(token, Claim.SELECTED_ROLE),
							getStringClaim(token, Claim.SELECTED_ORG)
					)
					.collect(Collectors.toUnmodifiableSet());

		} catch (Exception e) {

			LOGGER.error("Error extracting active groups from JWT", e);

			return Set.of();
		}
	}

	@Override
	@Cacheable(
			value = "authorization-permissions-cache",
			key = "#jwt",
			unless = "#result.isEmpty()"
	)
	public Set<String> getPermissions(
			String jwt,
			HttpServletRequest request
	) {

		try {

			LOGGER.debug("Extracting permissions from JWT");

			Jwt token = decode(jwt);

			return getStringSetClaim(token, Claim.PERMISSIONS);

		} catch (Exception e) {

			LOGGER.error("Error extracting permissions from JWT", e);

			return Set.of();
		}
	}

	/**
	 * Converters call the {@link Jwt} forms since 24.8/24.9. The interface defaults would call the String
	 * forms on {@code this}, a self-invocation that bypasses the cache proxy, so the cached overloads are
	 * declared here and read the claims of the token the resource server already decoded.
	 */
	@Override
	@Cacheable(value = "authorization-permissions-cache", key = "#jwt.tokenValue", unless = "#result.isEmpty()")
	public Set<String> getPermissions(Jwt jwt, HttpServletRequest request) {
		return getStringSetClaim(jwt, Claim.PERMISSIONS);
	}

	@Override
	@Cacheable(value = "authorization-super-admin-cache", key = "#jwt.tokenValue")
	public boolean isSuperAdmin(Jwt jwt, HttpServletRequest request) {
		Boolean superAdmin = jwt.getClaim(Claim.IS_SUPER_ADMIN.value());
		return Boolean.TRUE.equals(superAdmin);
	}

	@Override
	@Cacheable(value = "authorization-super-admin-cache", key = "#jwt")
	public boolean isSuperAdmin(
			String jwt,
			HttpServletRequest request
	) {

		try {

			LOGGER.debug("Checking super admin from JWT");

			Jwt token = decode(jwt);

			Boolean superAdmin =
					token.getClaim(
							Claim.IS_SUPER_ADMIN.value()
					);

			return Boolean.TRUE.equals(superAdmin);

		} catch (Exception e) {

			LOGGER.error("Error checking super admin from JWT", e);

			return false;
		}
	}

}
