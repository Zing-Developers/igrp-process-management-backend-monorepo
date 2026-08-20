package cv.igrp.framework.process.runtime.auth.irn;

import cv.igrp.framework.process.runtime.auth.core.CoreAuthorizationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Runs before the core auto-configuration so the IRN route adapter is registered first and the core
 * no-op fallback backs off.
 */
@Configuration
@AutoConfigureBefore(CoreAuthorizationAutoConfiguration.class)
@ComponentScan(basePackages = {
		"cv.igrp.framework.process.runtime.auth.irn.adapter",
})
@ConfigurationPropertiesScan
public class IRNAuthorizationAutoConfiguration {
}
