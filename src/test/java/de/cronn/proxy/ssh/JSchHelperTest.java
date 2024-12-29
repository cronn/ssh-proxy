package de.cronn.proxy.ssh;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class JSchHelperTest {

	@Test
	void testHostKeysPreferEcdsa() throws Exception {
		JSchHelper.reconfigureServerHostKeyOrder(JSchHelper.ServerHostKeySortOrder.PREFER_ECDSA);
		List<HostKeyType> expectedHostKeys = List.of(//
			HostKeyType.ECDSA256, //
			HostKeyType.ECDSA384, //
			HostKeyType.ECDSA521, //
			HostKeyType.SSH_RSA, //
			HostKeyType.SSH_DSS //
		);
		assertThat(JSchHelper.getServerHostKeys()).isEqualTo(expectedHostKeys);
	}

	@Test
	void testHostKeysPreferRsa() throws Exception {
		JSchHelper.reconfigureServerHostKeyOrder(JSchHelper.ServerHostKeySortOrder.PREFER_RSA);
		List<HostKeyType> expectedHostKeys = List.of(//
			HostKeyType.SSH_RSA, //
			HostKeyType.ECDSA256, //
			HostKeyType.ECDSA384, //
			HostKeyType.ECDSA521, //
			HostKeyType.SSH_DSS //
		);
		assertThat(expectedHostKeys).isEqualTo(JSchHelper.getServerHostKeys());
	}

}
