package cv.igrp.framework.process.runtime.auth.irn.adapter;

import cv.igrp.framework.process.runtime.auth.core.adapter.SuperAdminEmail;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.config.IrnApiProperties;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data.IrnMeResponse;
import cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data.UserSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Derives groups, permissions and super-admin status from the IRN {@code /Auth/me} response.
 *
 * <p>The response itself is cached per session by {@link IrnMeCache}, so all three lookups within a
 * request share a single call to IRN.
 */
@Service
@ConditionalOnProperty(
		name = "igrp.authorization.service.adapter",
		havingValue = "irn"
)
public class IrnAuthorizationCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IrnAuthorizationCacheService.class);

    private final IrnMeCache meCache;
    private final SuperAdminEmail superAdminEmail;

    public IrnAuthorizationCacheService(IrnMeCache meCache, IrnApiProperties properties) {
        this.meCache = meCache;
        this.superAdminEmail = new SuperAdminEmail(properties.superAdminEmail());
    }

    /**
     * Returns the current user's profile code plus the identifiers of the selected space, which are
     * mapped to Spring authorities as both roles and Activiti candidate groups.
     *
     * @param sessionId the IRN session id
     * @return the group identifiers, or an empty set when the user cannot be resolved
     */
    public Set<String> getGroups(String sessionId) {

        final var response = me(sessionId);

        if (response == null) {
            return Set.of();
        }

        final Set<String> groups = new HashSet<>(extractAllProfiles(response));
        groups.addAll(extractSelectedSpaceData(response));

        LOGGER.debug("Current user groups: {}", groups);

        return groups;
    }

    /**
     * Returns the current user's IRN permissions, in {@code MODULE:action} form, which become Spring
     * authorities verbatim.
     *
     * @param sessionId the IRN session id
     * @return the permissions, or an empty set when the user cannot be resolved
     */
    public Set<String> getPermissions(String sessionId) {

        final var response = me(sessionId);

        if (response == null || response.permissions() == null) {
            return Set.of();
        }

        final Set<String> permissions = new HashSet<>(response.permissions());

        LOGGER.debug("Current user permissions: {}", permissions);

        return permissions;
    }

    /**
     * Whether the current user is the configured super admin.
     *
     * @param sessionId the IRN session id
     * @return {@code true} when the user's email matches {@code irn.api.super-admin-email}
     *         (trimmed, case-insensitive)
     */
    public boolean isSuperAdmin(String sessionId) {

        final var response = me(sessionId);

        if (response == null || response.email() == null) {
            return false;
        }

        final var isSuperAdmin = superAdminEmail.matches(response.email());

        LOGGER.debug("Is current user super admin: {}", isSuperAdmin);

        return isSuperAdmin;
    }

    /**
     * Guards the cached lookup: the {@code @Cacheable} interceptor on {@link IrnMeCache#me} runs
     * before the method body and rejects a null key, so a request without the session cookie must be
     * short-circuited here. A missing cookie is a normal case (a super admin authenticated by JWT
     * alone, or any caller outside the IRN frontend), so it is logged at debug, not warn.
     */
    private IrnMeResponse me(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            LOGGER.debug("No IRN session id on the request; cannot resolve the current user");
            return null;
        }
        return meCache.me(sessionId);
    }

    /**
     * Extracts profile IDs from the IRN API response.
     * Currently only returns the selected profile. To include all user space profiles,
     * uncomment the code section below and modify the return statement.
     *
     * @param response the IRN API response
     * @return set of profile IDs
     */
    protected Set<String> extractAllProfiles(IrnMeResponse response) {
        if (response == null) {
            LOGGER.warn("extractAllProfiles: Received null response");
            return Set.of();
        }

        // Note: Currently only returning selected profile.
        // To include all user space profiles, uncomment this section:
        //
        // Stream<String> spaceProfiles =
        //     response.userSpaces() == null ? Stream.empty()
        //         : response.userSpaces().stream()
        //             .flatMap(space -> Stream.concat(
        //                 Stream.ofNullable(space.profileCode()),
        //                 space.profiles() == null
        //                     ? Stream.empty()
        //                     : space.profiles().stream()
        //                         .map(Profile::profileCode)
        //             ));

        Stream<String> selectedProfile =
                response.selectedProfile() != null
                        ? Stream.ofNullable(response.selectedProfile().profileCode())
                        : Stream.empty();

        return selectedProfile
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Extracts space-related data from the user's selected space.
     * Returns a list containing space ID, code, registry office ID, and conservatoria code.
     *
     * @param response the IRN API response
     * @return list of space-related identifiers, or empty list if not available
     */
    protected List<String> extractSelectedSpaceData(IrnMeResponse response) {

        if (response == null || response.selectedSpace() == null) {
            LOGGER.warn("extractSelectedSpaceData: Received null response or null selectedSpace");
            return List.of();
        }

        UserSpace space = response.selectedSpace();

        return Stream.of(
                        space.spaceId(),
                        space.spaceCode(),
                        space.registryOfficeId(),
                        space.conservatoriaCode()
                )
                .filter(Objects::nonNull)
                .toList();
    }
}
