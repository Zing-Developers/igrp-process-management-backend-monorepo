package cv.igrp.framework.process.runtime.auth.core.m2m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Introspects {@code igrpm2m_} API keys against the {@link M2mKeyResolver} SPI.
 *
 * <p>Plugs into the resource server as an {@link OpaqueTokenIntrospector}, so token extraction,
 * failure handling and 401 semantics are the standard bearer-token ones — an invalid M2M key fails
 * exactly like an invalid JWT ({@code invalid_token}).
 *
 * <p>The principal is {@code m2m:<clientName>} — always distinguishable from a human identity in
 * audit trails and attribution columns. Authorities are the key's {@code MODULE:action} permissions
 * (re-validated here as defence in depth: anything not matching the format, or any {@code ROLE_}/
 * {@code GROUP_} string, is dropped) plus the configured base authorities (e.g.
 * {@code ROLE_ACTIVITI_USER} where the engine requires it — never an admin role).
 *
 * <p>Fails closed: resolver exceptions become {@link BadOpaqueTokenException}; only the key prefix
 * is ever logged.
 */
public class M2mOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

	private static final Logger LOGGER = LoggerFactory.getLogger(M2mOpaqueTokenIntrospector.class);

	/** MODULE:action — uppercase module, lowercase action; roles can never smuggle through. */
	private static final Pattern PERMISSION_FORMAT = Pattern.compile("^[A-Z0-9_.]+:[a-z_]+$");

	public static final String PRINCIPAL_PREFIX = "m2m:";

	private final M2mKeyResolver resolver;
	private final List<String> baseAuthorities;

	public M2mOpaqueTokenIntrospector(M2mKeyResolver resolver, Collection<String> baseAuthorities) {
		this.resolver = resolver;
		this.baseAuthorities = List.copyOf(baseAuthorities);
	}

	@Override
	public OAuth2AuthenticatedPrincipal introspect(String token) {

		final M2mKey key;
		try {
			key = resolver.resolve(token)
					.orElseThrow(() -> new BadOpaqueTokenException("Unknown, revoked or expired M2M key"));
		} catch (BadOpaqueTokenException e) {
			LOGGER.warn("M2M authentication rejected for key prefix [{}]", prefixOf(token));
			throw e;
		} catch (RuntimeException e) {
			// fail closed: a store failure must never fall through to another authentication path
			LOGGER.error("M2M key resolution failed for key prefix [{}]: {}", prefixOf(token), e.getClass().getSimpleName());
			throw new BadOpaqueTokenException("M2M key resolution failed", e);
		}

		final var authorities = new ArrayList<GrantedAuthority>();
		final var granted = new LinkedHashSet<String>();
		for (String permission : key.permissions()) {
			if (permission != null && PERMISSION_FORMAT.matcher(permission.trim()).matches()) {
				granted.add(permission.trim());
			} else {
				LOGGER.warn("Dropping malformed M2M permission for client [{}]", key.clientName());
			}
		}
		granted.addAll(baseAuthorities);
		granted.forEach(a -> authorities.add(new SimpleGrantedAuthority(a)));

		final var principalName = PRINCIPAL_PREFIX + key.clientName();
		return new DefaultOAuth2AuthenticatedPrincipal(
				principalName,
				Map.of("sub", principalName, "client_name", key.clientName()),
				authorities);
	}

	private static String prefixOf(String token) {
		return token == null ? "" : token.substring(0, Math.min(token.length(), M2mKeyResolver.KEY_PREFIX.length() + 4));
	}

}
