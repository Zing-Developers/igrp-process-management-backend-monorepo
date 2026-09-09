package cv.igrp.framework.process.runtime.auth.core.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SuperAdminEmailTest {

	@Test
	void matchesTrimmedAndCaseInsensitively() {
		var email = new SuperAdminEmail("  Admin@Irn.cv ");
		assertThat(email.matches("admin@irn.cv")).isTrue();
		assertThat(email.matches(" ADMIN@IRN.CV ")).isTrue();
		assertThat(email.matches("admin@irn.cv.evil")).isFalse();
		assertThat(email.matches(null)).isFalse();
	}

	@Test
	void unconfiguredNeverMatches() {
		assertThat(new SuperAdminEmail("").matches("")).isFalse();
		assertThat(new SuperAdminEmail(null).matches("admin@irn.cv")).isFalse();
		assertThat(new SuperAdminEmail("   ").matches("   ")).isFalse();
	}

}
