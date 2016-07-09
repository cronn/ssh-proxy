package de.cronn.proxy.ssh;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jcraft.jsch.HostKey;

public class HostKeyTypeTest {

	@Test
	public void testGetType() throws Exception {
		assertEquals(HostKey.SSHRSA, HostKeyType.SSH_RSA.getType());
		assertEquals(HostKey.SSHDSS, HostKeyType.SSH_DSS.getType());

		for (HostKeyType hostKeyType : HostKeyType.values()) {
			assertTrue(hostKeyType.getType() > 0);
		}
	}

	@Test
	public void testGetTypeString() throws Exception {
		assertEquals("ssh-rsa", HostKeyType.SSH_RSA.getTypeString());
		assertEquals("ssh-dss", HostKeyType.SSH_DSS.getTypeString());

		for (HostKeyType hostKeyType : HostKeyType.values()) {
			assertNotNull(hostKeyType.getTypeString());
		}
	}

	@Test
	public void testByTypeString() throws Exception {
		assertEquals(HostKeyType.SSH_RSA, HostKeyType.byTypeString("ssh-rsa"));
		assertEquals(HostKeyType.SSH_DSS, HostKeyType.byTypeString("ssh-dss"));

		try {
			HostKeyType.byTypeString(null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("No hostKeyType found for null", e.getMessage());
		}

		try {
			HostKeyType.byTypeString("ssh-does-not-exist");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("No hostKeyType found for ssh-does-not-exist", e.getMessage());
		}
	}

}