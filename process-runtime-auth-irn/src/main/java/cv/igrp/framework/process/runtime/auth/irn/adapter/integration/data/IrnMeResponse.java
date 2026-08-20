package cv.igrp.framework.process.runtime.auth.irn.adapter.integration.data;

import java.util.List;

public record IrnMeResponse(
        String id,
        String email,
        String fullName,
        Boolean isAuthenticated,
        List<String> permissions,
        List<UserSpace> userSpaces,
        String selectedSpaceId,
        String selectedProfileId,
        UserSpace selectedSpace,
        Profile selectedProfile
) {}
