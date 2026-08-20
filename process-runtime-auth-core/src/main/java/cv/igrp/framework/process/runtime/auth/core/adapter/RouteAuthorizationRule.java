package cv.igrp.framework.process.runtime.auth.core.adapter;

import org.springframework.http.HttpMethod;

import java.util.Set;

/**
 * A single route authorization rule: a request matching {@code method} and {@code pattern} is allowed
 * when the caller holds at least one of {@code anyAuthority}.
 *
 * @param method       HTTP method to match, or {@code null} to match any method
 * @param pattern      Ant-style path pattern, e.g. {@code /tasks-instances/**}
 * @param anyAuthority authorities that grant access; holding any one of them is enough
 */
public record RouteAuthorizationRule(
		HttpMethod method,
		String pattern,
		Set<String> anyAuthority
) {
}
