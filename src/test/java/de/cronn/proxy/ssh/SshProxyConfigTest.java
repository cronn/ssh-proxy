package de.cronn.proxy.ssh;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jcraft.jsch.ConfigRepository.Config;

@ExtendWith(MockitoExtension.class)
class SshProxyConfigTest {

	@Mock
	private Config hostConfig;

	@Test
	void testParseProxyCommand_EmptyHostConfig() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		assertThat(sshProxyConfig.getForwardingHost()).isEqualTo("tunnel-host");
		assertThat(sshProxyConfig.getJumpHost()).isEqualTo("jumphost");
		assertThat(sshProxyConfig.getForwardingPort()).isEqualTo(22);
	}

	@Test
	void testParseProxyCommand_FullHostConfig() throws Exception {
		when(hostConfig.getHostname()).thenReturn("some-host");
		when(hostConfig.getPort()).thenReturn(1234);

		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W %h:%p jumphost", "tunnel-host", hostConfig);
		assertThat(sshProxyConfig.getForwardingHost()).isEqualTo("some-host");
		assertThat(sshProxyConfig.getJumpHost()).isEqualTo("jumphost");
		assertThat(sshProxyConfig.getForwardingPort()).isEqualTo(1234);
	}

	@Test
	void testParseProxyCommand_ConcreteHosts() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyCommand("ssh -q -W forwarding-host.123:5432 jumphost", "tunnel-host", hostConfig);
		assertThat(sshProxyConfig.getForwardingHost()).isEqualTo("forwarding-host.123");
		assertThat(sshProxyConfig.getJumpHost()).isEqualTo("jumphost");
		assertThat(sshProxyConfig.getForwardingPort()).isEqualTo(5432);
	}

	@Test
	void testParseProxyJump_EmptyHostConfig() throws Exception {
		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyJump("jumphost", "tunnel-host", hostConfig);
		assertThat(sshProxyConfig.getForwardingHost()).isEqualTo("tunnel-host");
		assertThat(sshProxyConfig.getJumpHost()).isEqualTo("jumphost");
		assertThat(sshProxyConfig.getForwardingPort()).isEqualTo(22);
	}

	@Test
	void testParseProxyJump_FullHostConfig() throws Exception {
		when(hostConfig.getHostname()).thenReturn("some-host");
		when(hostConfig.getPort()).thenReturn(1234);

		SshProxyConfig sshProxyConfig = SshProxyConfig.parseProxyJump("jumphost", "tunnel-host", hostConfig);
		assertThat(sshProxyConfig.getForwardingHost()).isEqualTo("some-host");
		assertThat(sshProxyConfig.getJumpHost()).isEqualTo("jumphost");
		assertThat(sshProxyConfig.getForwardingPort()).isEqualTo(1234);
	}

}
