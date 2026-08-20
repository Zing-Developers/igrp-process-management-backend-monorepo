package cv.igrp.framework.process.runtime.auth.core.adapter;

import java.util.List;

/**
 * Supplies the route authorization rules an application registers with Spring Security.
 *
 * <p>Keeps business routes out of the application's SecurityConfig: the application only iterates
 * over {@link #getRules()} and registers each one, so the route-to-permission mapping lives in the
 * authorization adapter that knows the identity provider's permission model.
 */
public interface IRouteAuthorizationAdapter {

	/**
	 * Rules to register, in order. Spring Security applies the first rule whose method and pattern
	 * match, so more specific rules must come before the broader ones.
	 *
	 * @return the ordered rules; empty when no route authorization is configured
	 */
	List<RouteAuthorizationRule> getRules();

	/**
	 * Whether requests not matched by any rule should be denied.
	 *
	 * @return {@code true} to deny unmatched requests (fail closed), {@code false} to only require
	 * authentication
	 */
	boolean denyUnmatched();

}
