package de.cronn.proxy.ssh;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.jcraft.jsch.HostKey;

class HostKeyTypeTest {

	@Test
	void testGetType() {
		assertThat(HostKeyType.SSH_RSA.getType()).isEqualTo(HostKey.SSHRSA);
		assertThat(HostKeyType.SSH_DSS.getType()).isEqualTo(HostKey.SSHDSS);

		for (HostKeyType hostKeyType : HostKeyType.values()) {
			assertThat(hostKeyType.getType()).isPositive();
		}
	}

	@Test
	void testGetTypeString() {
		assertThat(HostKeyType.SSH_RSA.getTypeString()).isEqualTo("ssh-rsa");
		assertThat(HostKeyType.SSH_DSS.getTypeString()).isEqualTo("ssh-dss");

		for (HostKeyType hostKeyType : HostKeyType.values()) {
			assertThat(hostKeyType.getTypeString()).isNotNull();
		}
	}

	@Test
	void testByTypeString() {
		assertThat(HostKeyType.byTypeString("ssh-rsa")).isEqualTo(HostKeyType.SSH_RSA);
		assertThat(HostKeyType.byTypeString("ssh-dss")).isEqualTo(HostKeyType.SSH_DSS);

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> HostKeyType.byTypeString(null))
			.withMessage("No hostKeyType found for null");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> HostKeyType.byTypeString("ssh-does-not-exist"))
			.withMessage("No hostKeyType found for ssh-does-not-exist");
	}

}
