package de.cronn.proxy.ssh;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class JSchHelperTest {

	@Test
	public void testHostKeysPreferEcdsa() throws Exception {
		JSchHelper.reconfigureServerHostKeyOrder(JSchHelper.ServerHostKeySortOrder.PREFER_ECDSA);
		List<HostKeyType> expectedHostKeys = Arrays.asList(//
			HostKeyType.ECDSA256, //
			HostKeyType.ECDSA384, //
			HostKeyType.ECDSA521, //
			HostKeyType.SSH_RSA, //
			HostKeyType.SSH_DSS //
		);
		assertEquals(expectedHostKeys, JSchHelper.getServerHostKeys());
	}

	@Test
	public void testHostKeysPreferRsa() throws Exception {
		JSchHelper.reconfigureServerHostKeyOrder(JSchHelper.ServerHostKeySortOrder.PREFER_RSA);
		List<HostKeyType> expectedHostKeys = Arrays.asList(//
			HostKeyType.SSH_RSA, //
			HostKeyType.ECDSA256, //
			HostKeyType.ECDSA384, //
			HostKeyType.ECDSA521, //
			HostKeyType.SSH_DSS //
		);
		assertEquals(expectedHostKeys, JSchHelper.getServerHostKeys());
	}

}