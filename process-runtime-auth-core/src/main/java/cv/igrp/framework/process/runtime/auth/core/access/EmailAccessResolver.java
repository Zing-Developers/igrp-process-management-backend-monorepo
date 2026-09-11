package cv.igrp.framework.process.runtime.auth.core.access;

import java.util.Set;

/**
 * SPI: permissions granted to the holder of a validated Keycloak token by the {@code email} claim,
 * for callers that have no IRN session (external systems with client-credentials tokens).
 *
 * <p>The application implements it over its own store (table + management endpoints); the framework
 * has no persistence, exactly like {@link cv.igrp.framework.process.runtime.auth.core.m2m.M2mKeyResolver}.
 * The email arrives already trimmed and lower-cased. An empty set means "no mapping": the caller keeps
 * whatever it had, which without a session is nothing. Whatever is returned is re-checked against
 * {@link cv.igrp.framework.process.runtime.auth.core.adapter.PermissionFormat} before it becomes an
 * authority, so a store can never grant a role or a group.
 *
 * <p>Exceptions propagate: the application's authorities converter fails closed on them.
 */
@FunctionalInterface
public interface EmailAccessResolver {

	Set<String> resolve(String email);

}
