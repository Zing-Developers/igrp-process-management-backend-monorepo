package cv.igrp.framework.process.runtime.auth.irn.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Route authorization configuration for the IRN adapter.
 *
 * <p>Each module maps a base path to an IRN module code. Permissions are derived from the HTTP method
 * ({@code GET} to {@code visualizar}, {@code POST} to {@code criar}, {@code PUT}/{@code PATCH} to
 * {@code editar}, {@code DELETE} to {@code eliminar}), producing authorities of the form
 * {@code MODULE:action}.
 *
 * <p>Routes that do not follow that rule are declared as overrides, for both reads issued as POST
 * (search endpoints taking the filter in the body) and sensitive capabilities that deserve their own
 * action, such as a process deployment.
 *
 * <pre>
 * irn.authorization.routes.deny-unmatched=true
 * irn.authorization.routes.modules[0].code=PROCESS_DEFINITIONS
 * irn.authorization.routes.modules[0].pattern=/process-definitions
 * irn.authorization.routes.modules[0].overrides[0].method=POST
 * irn.authorization.routes.modules[0].overrides[0].path=/deploy
 * irn.authorization.routes.modules[0].overrides[0].action=publicar
 * </pre>
 *
 * <p>Modules are matched in declaration order, so a module whose pattern is a prefix of another one
 * must be declared after it.
 *
 * @param denyUnmatched whether requests matching no rule are denied
 * @param modules       the modules, in matching order
 */
@ConfigurationProperties(prefix = "irn.authorization.routes")
public record IrnRouteProperties(
		@DefaultValue("true") boolean denyUnmatched,
		List<ModuleRoutes> modules
) {

	/**
	 * @param code      IRN module code, e.g. {@code PROCESS_DEFINITIONS}
	 * @param pattern   base path of the module's routes, e.g. {@code /process-definitions}
	 * @param overrides routes that escape the HTTP-method rule
	 */
	public record ModuleRoutes(
			String code,
			String pattern,
			List<Override> overrides
	) {

		/**
		 * @param method HTTP method of the route
		 * @param path   path suffix appended to the module pattern, e.g. {@code /deploy}
		 * @param action IRN action to require instead of the method-derived one, e.g. {@code publicar}
		 */
		public record Override(HttpMethod method, String path, String action) {
		}

	}

}
