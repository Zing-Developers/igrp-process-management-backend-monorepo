package cv.igrp.framework.process.runtime.auth.core.m2m;

import java.util.Set;

/**
 * A resolved machine-to-machine API key: the calling client and the permissions it carries.
 *
 * <p>Permissions are {@code MODULE:action} strings that plug straight into the route authorization
 * rules — never roles. The principal derived from a key is {@code m2m:<clientName>} so machine
 * actions stay distinguishable from human ones in audit trails.
 *
 * @param clientName  slug identifying the caller (e.g. {@code fila-trabalho-job})
 * @param permissions the {@code MODULE:action} authorities granted to the key
 */
public record M2mKey(String clientName, Set<String> permissions) {
}
