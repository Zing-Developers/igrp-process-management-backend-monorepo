package cv.igrp.framework.process.runtime.auth.core.adapter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionFormatTest {

	@Test
	void acceptsModuleActionOnly() {
		assertThat(PermissionFormat.isValid("TASK_INSTANCES:visualizar")).isTrue();
		assertThat(PermissionFormat.isValid("A.B_1:x_y")).isTrue();
		assertThat(PermissionFormat.isValid("  TASK_INSTANCES:visualizar ")).isTrue();
	}

	@Test
	void rejectsRolesGroupsAndMalformedStrings() {
		// ROLE_X:y passes the regex; the prefix check is what keeps roles out
		for (String bad : new String[]{"ROLE_DEPT_IGRP.superadmin", "DEPT_IGRP.superadmin", "GROUP_X",
				"ROLE_X:y", "GROUP_X:y", "task:View", "TASK:", ":x", "", "  ", null}) {
			assertThat(PermissionFormat.isValid(bad)).as("%s", bad).isFalse();
		}
	}

	@Test
	void onlyValidTrimsKeepsOrderAndDropsTheRest() {
		assertThat(PermissionFormat.onlyValid(Arrays.asList(" B:b", "ROLE_X:y", "A:a", null, "B:b")))
				.containsExactly("B:b", "A:a");
		assertThat(PermissionFormat.onlyValid(null)).isEmpty();
	}

}
