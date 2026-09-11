package cv.igrp.framework.process.runtime.auth.core;

import cv.igrp.framework.process.runtime.auth.core.access.EmailAccessResolver;
import cv.igrp.framework.process.runtime.auth.core.adapter.DefaultRouteAuthorizationAdapter;
import cv.igrp.framework.process.runtime.auth.core.adapter.IRouteAuthorizationAdapter;
import cv.igrp.framework.process.runtime.auth.core.m2m.M2mKeyResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Set;

@Configuration
@ComponentScan(basePackages = {
		"cv.igrp.framework.process.runtime.auth.core.adapter",
})
public class CoreAuthorizationAutoConfiguration {

	/**
	 * Falls back to no route rules when the active authorization adapter does not supply any, so
	 * applications can inject {@link IRouteAuthorizationAdapter} unconditionally.
	 */
	@Bean
	@ConditionalOnMissingBean(IRouteAuthorizationAdapter.class)
	public IRouteAuthorizationAdapter defaultRouteAuthorizationAdapter() {
		return new DefaultRouteAuthorizationAdapter();
	}

	/**
	 * Rejects every M2M key when the application supplies no credential store, so the M2M-aware
	 * authentication wiring can be unconditional: without a real resolver, an {@code igrpm2m_} token
	 * simply gets a 401.
	 */
	@Bean
	@ConditionalOnMissingBean(M2mKeyResolver.class)
	public M2mKeyResolver defaultM2mKeyResolver() {
		return rawKey -> Optional.empty();
	}

	/**
	 * Grants nothing by email when the application supplies no mapping store, so the adapters can
	 * inject {@link EmailAccessResolver} unconditionally: a session-less token simply keeps having no
	 * permissions.
	 */
	@Bean
	@ConditionalOnMissingBean(EmailAccessResolver.class)
	public EmailAccessResolver defaultEmailAccessResolver() {
		return email -> Set.of();
	}

}
