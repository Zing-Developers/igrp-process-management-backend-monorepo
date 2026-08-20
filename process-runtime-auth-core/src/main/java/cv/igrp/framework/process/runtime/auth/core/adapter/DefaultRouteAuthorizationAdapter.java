package cv.igrp.framework.process.runtime.auth.core.adapter;

import java.util.List;

/**
 * No-op route authorization: no rules, and unmatched requests only require authentication.
 *
 * <p>Registered as the fallback whenever the active authorization adapter does not provide route rules
 * of its own, so those deployments keep behaving exactly as before.
 */
public class DefaultRouteAuthorizationAdapter implements IRouteAuthorizationAdapter {

	@Override
	public List<RouteAuthorizationRule> getRules() {
		return List.of();
	}

	@Override
	public boolean denyUnmatched() {
		return false;
	}

}
