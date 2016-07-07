package de.cronn.proxy.ssh;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.cronn.proxy.ssh.util.Assert;

public class SshProxy implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(SshProxy.class);

	public static final String LOCALHOST = "localhost";

	private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;

	private final Deque<Session> sshSessions = new ArrayDeque<>();
	private final Map<Session, Set<Integer>> portForwardings = new LinkedHashMap<>();

	private final SshConfiguration sshConfiguration;
	private int timeoutMillis;

	public SshProxy() {
		this(DEFAULT_TIMEOUT_MILLIS);
	}

	public SshProxy(int timeoutMillis) {
		try {
			sshConfiguration = SshConfiguration.getConfiguration();
		} catch (Exception e) {
			throw new RuntimeException("Failed to open SSH proxy", e);
		}
		this.timeoutMillis = timeoutMillis;
	}

	public int connect(String sshTunnelHost, String host, int port) {
		Assert.notNull(sshTunnelHost, "sshTunnelHost must not be null");
		Assert.notNull(host, "host must not be null");

		log.debug("tunneling to {}:{} via {}", host, port, sshTunnelHost);

		try {
			sshConfiguration.addIdentity(sshTunnelHost);

			SshProxyCommand proxyConfig = sshConfiguration.getProxyCommandConfiguration(sshTunnelHost);
			if (proxyConfig == null) {
				return directConnect(sshTunnelHost, host, port);
			}

			int jumpPort = connect(proxyConfig);

			String hostUser = sshConfiguration.getHostUser(sshTunnelHost);
			String jumpHost = proxyConfig.getJumpHost();
			Session jumpHostSession = sshConfiguration.openSession(hostUser, jumpHost, jumpPort);
			String hostname = sshConfiguration.getHostName(sshTunnelHost);
			jumpHostSession.setHostKeyAlias(hostname);
			sshSessions.push(jumpHostSession);
			jumpHostSession.setTimeout(timeoutMillis);
			jumpHostSession.connect(timeoutMillis);

			log.debug("[{}] connected via {}@localhost:{}", sshTunnelHost, hostUser, jumpPort);

			return addLocalPortForwarding(sshTunnelHost, jumpHostSession, host, port);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create SSH tunnel", e);
		}
	}

	private int connect(SshProxyCommand proxyConfig) {
		String jumpHost = proxyConfig.getJumpHost();
		String forwardingHost = proxyConfig.getForwardingHost();
		int forwardingPort = proxyConfig.getForwardingPort();
		return connect(jumpHost, forwardingHost, forwardingPort);
	}

	private int directConnect(String jumpHost, String targetHost, int targetPort) throws JSchException {
		Session jumpHostSession = sshConfiguration.openSession(jumpHost);
		sshSessions.add(jumpHostSession);
		jumpHostSession.setTimeout(timeoutMillis);
		jumpHostSession.connect(timeoutMillis);

		log.debug("[{}] connected", jumpHost);

		return addLocalPortForwarding(jumpHost, jumpHostSession, targetHost, targetPort);
	}

	private int addLocalPortForwarding(String sshTunnelHost, Session session, String targetHost, int targetPort) throws JSchException {
		int localPort = session.setPortForwardingL(0, targetHost, targetPort);

		log.debug("[{}] local port {} forwarded to {}:{}", sshTunnelHost, localPort, targetHost, targetPort);

		Set<Integer> ports = portForwardings.get(session);
		if (ports == null) {
			ports = new LinkedHashSet<>();
			portForwardings.put(session, ports);
		}
		ports.add(Integer.valueOf(localPort));
		return localPort;
	}

	@Override
	public void close() {
		if (!sshSessions.isEmpty()) {
			log.debug("closing SSH sessions");
		}

		while (!sshSessions.isEmpty()) {
			Session session = sshSessions.pop();

			deletePortForwarding(session);

			try {
				session.disconnect();
			} catch (Exception e) {
				log.error("Failed to disconnect SSH session", e);
			}
		}

		Assert.isTrue(portForwardings.isEmpty(), "port forwardings must be empty at this point");
	}

	private void deletePortForwarding(Session session) {
		Set<Integer> ports = portForwardings.remove(session);
		if (ports != null) {
			for (Integer localPort : ports) {
				try {
					String host = session.getHost();
					if (host.equals(LOCALHOST)) {
						host = session.getHostKeyAlias();
					}
					session.delPortForwardingL(LOCALHOST, localPort.intValue());
					log.debug("deleted local port forwarding on port {} for {}", localPort, host);
				} catch (Exception e) {
					log.error("failed to delete port forwarding of port {}", localPort, e);
				}
			}
		}
	}

}
