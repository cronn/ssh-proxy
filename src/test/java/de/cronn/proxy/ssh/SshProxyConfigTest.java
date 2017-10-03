package de.cronn.proxy.ssh;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.jcraft.jsch.ConfigRepository.Config;

@RunWith(MockitoJUnitRunner.class)
public class SshProxyConfigTest {

	@Mock
	private Config hostConfig;

	@Test
	public void testParse_EmptyHostConfig() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parse("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		Assert.assertEquals("tunnel-host", sshProxyConfig.getForwardingHost());
		Assert.assertEquals("jumphost", sshProxyConfig.getJumpHost());
		Assert.assertEquals(22, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParse_FullHostConfig() throws Exception {
		when(hostConfig.getHostname()).thenReturn("some-host");
		when(hostConfig.getPort()).thenReturn(1234);

		SshProxyConfig sshProxyConfig = SshProxyConfig.parse("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		Assert.assertEquals("some-host", sshProxyConfig.getForwardingHost());
		Assert.assertEquals("jumphost", sshProxyConfig.getJumpHost());
		Assert.assertEquals(1234, sshProxyConfig.getForwardingPort());
	}

	@Test
	public void testParse_ConcreteHosts() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parse("ssh -q -W forwarding-host.123:5432 jumphost", "tunnel-host", hostConfig);
		Assert.assertEquals("forwarding-host.123", sshProxyConfig.getForwardingHost());
		Assert.assertEquals("jumphost", sshProxyConfig.getJumpHost());
		Assert.assertEquals(5432, sshProxyConfig.getForwardingPort());
	}

}