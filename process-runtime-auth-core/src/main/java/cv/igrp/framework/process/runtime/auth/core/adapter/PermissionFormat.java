package cv.igrp.framework.process.runtime.auth.core.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The one shape a permission may take before it becomes a Spring authority: {@code MODULE:action},
 * uppercase module, lowercase action. Anything else is dropped, including {@code ROLE_*} and
 * {@code GROUP_*} strings (the regex alone would accept {@code ROLE_X:y}), so no store the application
 * owns can hand out the super-admin role that the route rules append everywhere.
 */
public final class PermissionFormat {

	private static final Logger LOGGER = LoggerFactory.getLogger(PermissionFormat.class);

	private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9_.]+:[a-z_]+$");

	private PermissionFormat() {
	}

	/** Whether {@code permission} (trimmed) is a well-formed {@code MODULE:action} and not a role or group. */
	public static boolean isValid(String permission) {
		if (permission == null) return false;
		final var value = permission.trim();
		return FORMAT.matcher(value).matches()
				&& !value.startsWith("ROLE_")
				&& !value.startsWith("GROUP_");
	}

	/** The valid entries of {@code permissions}, trimmed, in order; the rest are logged and dropped. */
	public static Set<String> onlyValid(Collection<String> permissions) {
		final var valid = new LinkedHashSet<String>();
		if (permissions == null) return valid;
		for (String permission : permissions) {
			if (isValid(permission)) {
				valid.add(permission.trim());
			} else {
				LOGGER.warn("Dropping malformed permission (expected MODULE:action, roles are not allowed)");
			}
		}
		return valid;
	}

}
