package de.cronn.proxy.ssh;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jcraft.jsch.ConfigRepository.Config;

@RunWith(MockitoJUnitRunner.class)
public class SshProxyConfigTest {

	@Mock
	private Config hostConfig;

	@Test
	public void testParseProxyCommand_EmptyHostConfig() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		assertEquals("tunnel-host", sshProxyConfig.getForwardingHost());
		assertEquals("jumphost", sshProxyConfig.getJumpHost());
		assertEquals(22, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParseProxyCommand_FullHostConfig() throws Exception {
		when(hostConfig.getHostname()).thenReturn("some-host");
		when(hostConfig.getPort()).thenReturn(1234);

		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		assertEquals("some-host", sshProxyConfig.getForwardingHost());
		assertEquals("jumphost", sshProxyConfig.getJumpHost());
		assertEquals(1234, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParseProxyCommand_ConcreteHosts() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W forwarding-host.123:5432 jumphost", "tunnel-host", hostConfig);
		assertEquals("forwarding-host.123", sshProxyConfig.getForwardingHost());
		assertEquals("jumphost", sshProxyConfig.getJumpHost());
		assertEquals(5432, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParseProxyJump_EmptyHostConfig() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyJump("jumphost", "tunnel-host", hostConfig);
		assertEquals("tunnel-host", sshProxyConfig.getForwardingHost());
		assertEquals("jumphost", sshProxyConfig.getJumpHost());
		assertEquals(22, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParseProxyJump_FullHostConfig() throws Exception {
		when(hostConfig.getHostname()).thenReturn("some-host");
		when(hostConfig.getPort()).thenReturn(1234);

		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyJump("jumphost", "tunnel-host", hostConfig);
		assertEquals("some-host", sshProxyConfig.getForwardingHost());
		assertEquals("jumphost", sshProxyConfig.getJumpHost());
		assertEquals(1234, sshProxyConfig.getForwardingPort());
	}

}