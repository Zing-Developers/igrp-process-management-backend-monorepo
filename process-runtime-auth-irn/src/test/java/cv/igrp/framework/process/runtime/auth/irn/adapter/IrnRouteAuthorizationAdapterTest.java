package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.RouteAuthorizationRule;
import cv.igrp.framework.process.runtime.auth.irn.adapter.IrnRouteProperties.ModuleRoutes;
import cv.igrp.framework.process.runtime.auth.irn.adapter.IrnRouteProperties.ModuleRoutes.Override;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IrnRouteAuthorizationAdapterTest {

	private static IrnRouteAuthorizationAdapter adapterFor(ModuleRoutes... modules) {
		return new IrnRouteAuthorizationAdapter(new IrnRouteProperties(true, List.of(modules)));
	}

	private static boolean hasRule(List<RouteAuthorizationRule> rules,
	                               HttpMethod method, String pattern, String authority) {
		return rules.stream().anyMatch(r -> method.equals(r.method())
				&& pattern.equals(r.pattern())
				&& Set.of(authority).equals(r.anyAuthority()));
	}

	private static int indexOf(List<RouteAuthorizationRule> rules, HttpMethod method, String pattern) {
		for (int i = 0; i < rules.size(); i++) {
			if (method.equals(rules.get(i).method()) && pattern.equals(rules.get(i).pattern())) {
				return i;
			}
		}
		return -1;
	}

	@Test
	void derivesOneRuleForEachMethodOverBothTheBasePathAndEverythingBelowIt() {

		var rules = adapterFor(new ModuleRoutes("AREAS", "/areas", List.of())).getRules();

		assertThat(hasRule(rules, HttpMethod.GET, "/areas", "AREAS:visualizar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.POST, "/areas/**", "AREAS:criar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.PUT, "/areas/**", "AREAS:editar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.PATCH, "/areas/**", "AREAS:editar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.DELETE, "/areas/**", "AREAS:eliminar")).isTrue();
	}

	@Test
	void overrideWinsBecauseItIsEmittedBeforeTheModuleGenericRules() {

		// Without this ordering, POST /process-definitions/deploy would match the generic POST rule and
		// :criar would be enough to publish executable BPMN.
		var rules = adapterFor(new ModuleRoutes("PROCESS_DEFINITIONS", "/process-definitions",
				List.of(new Override(HttpMethod.POST, "/deploy", "publicar")))).getRules();

		var deployIndex = indexOf(rules, HttpMethod.POST, "/process-definitions/deploy");
		var genericIndex = indexOf(rules, HttpMethod.POST, "/process-definitions/**");

		assertThat(deployIndex).isNotNegative();
		assertThat(genericIndex).isNotNegative();
		assertThat(deployIndex).isLessThan(genericIndex);

		assertThat(hasRule(rules, HttpMethod.POST, "/process-definitions/deploy",
				"PROCESS_DEFINITIONS:publicar")).isTrue();
	}

	@Test
	void readOverrideKeepsSearchOutOfTheWritePermission() {

		var rules = adapterFor(new ModuleRoutes("TASK_INSTANCES", "/tasks-instances",
				List.of(new Override(HttpMethod.POST, "/search", "visualizar")))).getRules();

		assertThat(hasRule(rules, HttpMethod.POST, "/tasks-instances/search",
				"TASK_INSTANCES:visualizar")).isTrue();
	}

	@Test
	void keepsModulesInDeclarationOrderSoAPrefixModuleCannotSwallowASpecificOne() {

		var rules = adapterFor(
				new ModuleRoutes("STUDIO_PROCESS_DEFINITIONS", "/api/v1/projects/process-definitions", List.of()),
				new ModuleRoutes("STUDIO_PROJECTS", "/api/v1/projects", List.of())
		).getRules();

		var specific = indexOf(rules, HttpMethod.GET, "/api/v1/projects/process-definitions/**");
		var prefix = indexOf(rules, HttpMethod.GET, "/api/v1/projects/**");

		assertThat(specific).isLessThan(prefix);
	}

	@Test
	void normalizesMissingLeadingSlashesAndTrailingSlashes() {

		var rules = adapterFor(new ModuleRoutes("AREAS", "areas/",
				List.of(new Override(HttpMethod.POST, "search", "visualizar")))).getRules();

		assertThat(hasRule(rules, HttpMethod.POST, "/areas/search", "AREAS:visualizar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
	}

	@Test
	void failsLoudlyOnAnIncompleteModuleInsteadOfEmittingABrokenRule() {

		assertThatThrownBy(() -> adapterFor(new ModuleRoutes("AREAS", null, List.of())).getRules())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("pattern");

		assertThatThrownBy(() -> adapterFor(new ModuleRoutes(null, "/areas", List.of())).getRules())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("code");

		assertThatThrownBy(() -> adapterFor(new ModuleRoutes("AREAS", "/areas",
				List.of(new Override(HttpMethod.POST, "/deploy", null)))).getRules())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("action");
	}

	@Test
	void toleratesAnEmptyConfigurationInsteadOfFailingStartup() {

		var noModules = new IrnRouteAuthorizationAdapter(new IrnRouteProperties(true, null));

		assertThat(noModules.getRules()).isEmpty();
		assertThat(noModules.denyUnmatched()).isTrue();
	}

	@Test
	void toleratesNullOverrides() {

		var rules = adapterFor(new ModuleRoutes("AREAS", "/areas", null)).getRules();

		assertThat(hasRule(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
	}

}
