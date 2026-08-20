package cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data;

import java.util.List;

public record UserSpace(
        String spaceId,
        String spaceCode,
        String spaceName,
        String registryOfficeId,
        String conservatoriaCode,
        String conservatoriaName,
        Boolean isPrimary,
        String profileId,
        String profileName,
        String userProfileSpaceId,
        List<Profile> profiles
) {}