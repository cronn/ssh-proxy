package de.cronn.proxy.ssh;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ConfigRepository.Config;

import de.cronn.proxy.ssh.util.Assert;

public class SshProxyConfig {

	private static final Logger log = LoggerFactory.getLogger(SshProxyConfig.class);

	private static final Pattern SSH_PROXY_COMMAND_PATTERN = Pattern.compile("ssh -q -W ([\\w\\.\\-_0-9]+|%h):(\\d+|%p) (.+)");

	private final int forwardingPort;
	private final String forwardingHost;
	private final String jumpHost;

	public SshProxyConfig(int forwardingPort, String forwardingHost, String jumpHost) {
		this.forwardingPort = forwardingPort;
		this.forwardingHost = forwardingHost;
		this.jumpHost = jumpHost;
	}

	public static SshProxyConfig parse(String proxyCommandConfig, String sshTunnelHost, Config hostConfig) {
		Matcher matcher = SSH_PROXY_COMMAND_PATTERN.matcher(proxyCommandConfig);
		Assert.isTrue(matcher.matches(),
			"Illegal ProxyCommand configured for host " + sshTunnelHost + ": " //
				+ proxyCommandConfig + "." //
				+ " Please check your SSH configuration in " + SshConfiguration.getLocalSshConfigPath());

		log.debug("[{}] emulating proxy command: {}", sshTunnelHost, proxyCommandConfig);

		String forwardingHost = matcher.group(1);
		if (forwardingHost.equals("%h")) {
			forwardingHost = hostConfig.getHostname();
			if (forwardingHost == null) {
				forwardingHost = sshTunnelHost;
			}
		}
		String portConfig = matcher.group(2);
		final int forwardingPort;
		if (portConfig.equals("%p")) {
			int port = hostConfig.getPort();
			if (port <= 0) {
				port = SshConfiguration.SSH_DEFAULT_PORT;
			}
			forwardingPort = port;
		} else {
			forwardingPort = Integer.parseInt(portConfig);
		}
		String jumpHost = matcher.group(3);

		return new SshProxyConfig(forwardingPort, forwardingHost, jumpHost);
	}

	public int getForwardingPort() {
		return forwardingPort;
	}

	public String getForwardingHost() {
		return forwardingHost;
	}

	public String getJumpHost() {
		return jumpHost;
	}
}
