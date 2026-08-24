package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.IRouteAuthorizationAdapter;
import cv.igrp.framework.process.runtime.auth.core.adapter.RouteAuthorizationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives Spring Security route rules from {@link IrnRouteProperties}, using IRN permissions of the
 * form {@code MODULE:action}.
 *
 * <p>For each configured module, in declaration order: the overrides first, then one rule per HTTP
 * method over the module's base path and everything below it. Overrides come first because that is
 * where the conflict is, and Spring Security applies the first matching rule.
 */
@Component
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "irn"
)
public class IrnRouteAuthorizationAdapter implements IRouteAuthorizationAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(IrnRouteAuthorizationAdapter.class);

	/** HTTP method to IRN action. Order is the order rules are emitted in. */
	private static final Map<HttpMethod, String> ACTION_BY_METHOD = new LinkedHashMap<>();

	static {
		ACTION_BY_METHOD.put(HttpMethod.GET, "visualizar");
		ACTION_BY_METHOD.put(HttpMethod.POST, "criar");
		ACTION_BY_METHOD.put(HttpMethod.PUT, "editar");
		ACTION_BY_METHOD.put(HttpMethod.PATCH, "editar");
		ACTION_BY_METHOD.put(HttpMethod.DELETE, "eliminar");
	}

	private final IrnRouteProperties properties;

	public IrnRouteAuthorizationAdapter(IrnRouteProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<RouteAuthorizationRule> getRules() {

		final var modules = properties.modules();

		if (modules == null || modules.isEmpty()) {
			LOGGER.warn("No route authorization modules configured under 'irn.authorization.routes.modules'. "
					+ "With deny-unmatched={} every business route will be {}.",
					properties.denyUnmatched(), properties.denyUnmatched() ? "denied" : "left authenticated-only");
			return List.of();
		}

		final List<RouteAuthorizationRule> rules = new ArrayList<>();

		for (var module : modules) {
			rules.addAll(overrideRules(module));
			rules.addAll(methodRules(module));
		}

		LOGGER.debug("Built {} route authorization rules from {} modules", rules.size(), modules.size());

		return List.copyOf(rules);
	}

	@Override
	public boolean denyUnmatched() {
		return properties.denyUnmatched();
	}

	/**
	 * Rules for routes that escape the HTTP-method rule, such as searches issued as POST or a
	 * deployment that has its own action.
	 */
	private List<RouteAuthorizationRule> overrideRules(IrnRouteProperties.ModuleRoutes module) {

		final var overrides = module.overrides();

		if (overrides == null || overrides.isEmpty()) {
			return List.of();
		}

		final List<RouteAuthorizationRule> rules = new ArrayList<>(overrides.size());

		for (var override : overrides) {
			rules.add(new RouteAuthorizationRule(
					override.method(),
					basePath(module) + normalizeSuffix(override.path()),
					authoritiesFor(module, override.action())
			));
		}

		return rules;
	}

	/**
	 * One rule per HTTP method, over the module's base path and everything below it. Both patterns are
	 * needed because controllers may expose mappings with no path, which match the base path itself.
	 */
	private List<RouteAuthorizationRule> methodRules(IrnRouteProperties.ModuleRoutes module) {

		final var base = basePath(module);
		final List<RouteAuthorizationRule> rules = new ArrayList<>(ACTION_BY_METHOD.size() * 2);

		for (var entry : ACTION_BY_METHOD.entrySet()) {
			final var authorities = authoritiesFor(module, entry.getValue());
			rules.add(new RouteAuthorizationRule(entry.getKey(), base, authorities));
			rules.add(new RouteAuthorizationRule(entry.getKey(), base + "/**", authorities));
		}

		return rules;
	}

	/**
	 * The authorities that grant a route: the module's derived {@code code:action}, plus any real IRN
	 * permissions declared under {@code accept-also} for that action (the several frontend modules that
	 * front the same endpoints). Any one of them passes.
	 */
	private static Set<String> authoritiesFor(IrnRouteProperties.ModuleRoutes module, String action) {
		final var authorities = new LinkedHashSet<String>();
		authorities.add(authority(module.code(), action));

		final var acceptAlso = module.acceptAlso();
		if (acceptAlso != null) {
			final var extra = acceptAlso.get(action);
			if (extra != null) {
				extra.stream()
						.filter(p -> p != null && !p.isBlank())
						.map(String::trim)
						.forEach(authorities::add);
			}
		}
		return Set.copyOf(authorities);
	}

	private static String basePath(IrnRouteProperties.ModuleRoutes module) {
		final var pattern = module.pattern();
		if (pattern == null || pattern.isBlank()) {
			throw new IllegalStateException(
					"Missing 'pattern' for route authorization module " + module.code());
		}
		final var trimmed = pattern.trim();
		final var withLeadingSlash = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
		return withLeadingSlash.endsWith("/")
				? withLeadingSlash.substring(0, withLeadingSlash.length() - 1)
				: withLeadingSlash;
	}

	private static String normalizeSuffix(String path) {
		if (path == null || path.isBlank()) {
			return "";
		}
		final var trimmed = path.trim();
		return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
	}

	private static String authority(String moduleCode, String action) {
		if (moduleCode == null || moduleCode.isBlank()) {
			throw new IllegalStateException("Missing 'code' for a route authorization module");
		}
		if (action == null || action.isBlank()) {
			throw new IllegalStateException("Missing 'action' for an override of module " + moduleCode);
		}
		return moduleCode.trim() + ":" + action.trim();
	}

}
