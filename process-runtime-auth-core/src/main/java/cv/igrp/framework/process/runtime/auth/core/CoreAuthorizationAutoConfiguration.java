package cv.igrp.framework.process.runtime.auth.core;

import cv.igrp.framework.process.runtime.auth.core.adapter.DefaultRouteAuthorizationAdapter;
import cv.igrp.framework.process.runtime.auth.core.adapter.IRouteAuthorizationAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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

}
