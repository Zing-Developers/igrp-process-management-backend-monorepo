package cv.igrp.framework.process.runtime.irn.integration;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ComponentScan(basePackages = {
		"cv.igrp.framework.process.runtime.irn.integration",
})
@ConfigurationPropertiesScan
public class IRNIntegrationAutoConfiguration {
}
