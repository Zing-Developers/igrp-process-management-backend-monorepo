package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.RouteAuthorizationRule;
import cv.igrp.framework.process.runtime.auth.irn.adapter.IrnRouteProperties.ModuleRoutes;
import cv.igrp.framework.process.runtime.auth.irn.adapter.IrnRouteProperties.ModuleRoutes.Override;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IrnRouteAuthorizationAdapterTest {

	private static ModuleRoutes mod(String code, String pattern, List<Override> overrides) {
		return new ModuleRoutes(code, pattern, overrides, Map.of());
	}

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

		var rules = adapterFor(mod("AREAS", "/areas", List.of())).getRules();

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
		var rules = adapterFor(mod("PROCESS_DEFINITIONS", "/process-definitions",
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

		var rules = adapterFor(mod("TASK_INSTANCES", "/tasks-instances",
				List.of(new Override(HttpMethod.POST, "/search", "visualizar")))).getRules();

		assertThat(hasRule(rules, HttpMethod.POST, "/tasks-instances/search",
				"TASK_INSTANCES:visualizar")).isTrue();
	}

	@Test
	void keepsModulesInDeclarationOrderSoAPrefixModuleCannotSwallowASpecificOne() {

		var rules = adapterFor(
				mod("STUDIO_PROCESS_DEFINITIONS", "/api/v1/projects/process-definitions", List.of()),
				mod("STUDIO_PROJECTS", "/api/v1/projects", List.of())
		).getRules();

		var specific = indexOf(rules, HttpMethod.GET, "/api/v1/projects/process-definitions/**");
		var prefix = indexOf(rules, HttpMethod.GET, "/api/v1/projects/**");

		assertThat(specific).isLessThan(prefix);
	}

	@Test
	void normalizesMissingLeadingSlashesAndTrailingSlashes() {

		var rules = adapterFor(mod("AREAS", "areas/",
				List.of(new Override(HttpMethod.POST, "search", "visualizar")))).getRules();

		assertThat(hasRule(rules, HttpMethod.POST, "/areas/search", "AREAS:visualizar")).isTrue();
		assertThat(hasRule(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
	}

	@Test
	void failsLoudlyOnAnIncompleteModuleInsteadOfEmittingABrokenRule() {

		assertThatThrownBy(() -> adapterFor(mod("AREAS", null, List.of())).getRules())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("pattern");

		assertThatThrownBy(() -> adapterFor(mod(null, "/areas", List.of())).getRules())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("code");

		assertThatThrownBy(() -> adapterFor(mod("AREAS", "/areas",
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

		var rules = adapterFor(mod("AREAS", "/areas", null)).getRules();

		assertThat(hasRule(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
	}

	private static boolean acceptsAnyOf(java.util.List<RouteAuthorizationRule> rules,
	                                    HttpMethod method, String pattern, String... authorities) {
		return rules.stream().anyMatch(r -> method.equals(r.method())
				&& pattern.equals(r.pattern())
				&& r.anyAuthority().equals(Set.of(authorities)));
	}

	@Test
	void acceptAlsoAddsTheFrontendPermissionsAlongsideTheDerivedOne() {
		// The same /tasks-instances endpoints are fronted by several IRN modules, each with its own code
		// and verb; a read route must pass for ANY of them, plus our own derived permission.
		var module = new ModuleRoutes("TASK_INSTANCES", "/tasks-instances",
				List.of(new Override(HttpMethod.POST, "/search", "visualizar")),
				Map.of(
						"visualizar", List.of("FILA_TRABALHO:visualizar", "TASK_MANAGEMENT:ver"),
						"criar", List.of("FILA_TRABALHO:executar")));

		var rules = adapterFor(module).getRules();

		// generic GET tier: derived + both read frontends
		assertThat(acceptsAnyOf(rules, HttpMethod.GET, "/tasks-instances/**",
				"TASK_INSTANCES:visualizar", "FILA_TRABALHO:visualizar", "TASK_MANAGEMENT:ver")).isTrue();
		// the POST /search override INHERITS the visualizar list (keyed by action)
		assertThat(acceptsAnyOf(rules, HttpMethod.POST, "/tasks-instances/search",
				"TASK_INSTANCES:visualizar", "FILA_TRABALHO:visualizar", "TASK_MANAGEMENT:ver")).isTrue();
		// write tier: derived + the operate frontend only
		assertThat(acceptsAnyOf(rules, HttpMethod.POST, "/tasks-instances/**",
				"TASK_INSTANCES:criar", "FILA_TRABALHO:executar")).isTrue();
		// a tier with no accept-also entry keeps only the derived permission
		assertThat(acceptsAnyOf(rules, HttpMethod.DELETE, "/tasks-instances/**",
				"TASK_INSTANCES:eliminar")).isTrue();
	}

	@Test
	void withoutAcceptAlsoTheRulesAreIdenticalToBefore() {
		var rules = adapterFor(mod("AREAS", "/areas", List.of())).getRules();
		// each rule carries exactly the single derived authority — no behavioural change
		assertThat(acceptsAnyOf(rules, HttpMethod.GET, "/areas/**", "AREAS:visualizar")).isTrue();
		assertThat(acceptsAnyOf(rules, HttpMethod.POST, "/areas/**", "AREAS:criar")).isTrue();
	}

}
