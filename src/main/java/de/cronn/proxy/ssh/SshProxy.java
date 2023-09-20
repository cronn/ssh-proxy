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

	private final String passphrase;

	public SshProxy() {
		this(DEFAULT_TIMEOUT_MILLIS, null);
	}

	public SshProxy(String passphrase) {
		this(DEFAULT_TIMEOUT_MILLIS, passphrase);
	}

	public SshProxy(int timeoutMillis) {
		this(timeoutMillis, null);
	}

	public SshProxy(int timeoutMillis, String passphrase) {
		this.passphrase = passphrase;
		try {
			sshConfiguration = SshConfiguration.getConfiguration();
		} catch (Exception e) {
			throw new SshProxyRuntimeException("Failed to open SSH proxy", e);
		}
		this.timeoutMillis = timeoutMillis;
	}

	public int connect(String sshTunnelHost, String host, int port) {
		return connect(sshTunnelHost, host, port, 0);
	}

	public int connect(String sshTunnelHost, String host, int port, int localPort) {
		Assert.notNull(sshTunnelHost, "sshTunnelHost must not be null");
		Assert.notNull(host, "host must not be null");
		Assert.isTrue(port > 0, "illegal port: " + port);
		Assert.isTrue(localPort >= 0, "illegal local port: " + localPort);

		log.debug("tunneling to {}:{} via {}", host, port, sshTunnelHost);

		try {
			if (passphrase != null && !passphrase.isEmpty()) {
				sshConfiguration.addIdentity(sshTunnelHost, passphrase);
			} else {
				sshConfiguration.addIdentity(sshTunnelHost);
			}

			SshProxyConfig proxyConfig = sshConfiguration.getProxyConfiguration(sshTunnelHost);
			if (proxyConfig == null) {
				return directConnect(sshTunnelHost, host, port, localPort);
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

			return addLocalPortForwarding(sshTunnelHost, jumpHostSession, host, port, localPort);
		} catch (Exception e) {
			throw new SshProxyRuntimeException("Failed to create SSH tunnel to " + host + " via " + sshTunnelHost, e);
		}
	}

	private int connect(SshProxyConfig proxyConfig) {
		String jumpHost = proxyConfig.getJumpHost();
		String forwardingHost = proxyConfig.getForwardingHost();
		int forwardingPort = proxyConfig.getForwardingPort();
		return connect(jumpHost, forwardingHost, forwardingPort);
	}

	private int directConnect(String jumpHost, String targetHost, int targetPort, int localPort) throws JSchException {
		Session jumpHostSession = sshConfiguration.openSession(jumpHost);
		sshSessions.add(jumpHostSession);
		jumpHostSession.setTimeout(timeoutMillis);
		try {
			jumpHostSession.connect(timeoutMillis);
		} catch (JSchException e) {
			log.debug("Failed to connect to {} via {}", targetHost, jumpHost, e);
			throw new SshProxyRuntimeException("Failed to connect to " + targetHost + " via " + jumpHost);
		}

		log.debug("[{}] connected", jumpHost);

		return addLocalPortForwarding(jumpHost, jumpHostSession, targetHost, targetPort, localPort);
	}

	private int addLocalPortForwarding(String sshTunnelHost, Session session, String targetHost, int targetPort, int localPort) throws JSchException {
		int localPortReturned = session.setPortForwardingL(localPort, targetHost, targetPort);

		log.debug("[{}] local port {} forwarded to {}:{}", sshTunnelHost, localPortReturned, targetHost, targetPort);

		Set<Integer> ports = portForwardings.computeIfAbsent(session, k -> new LinkedHashSet<>());
		ports.add(Integer.valueOf(localPortReturned));
		return localPortReturned;
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
				deletePortForwarding(session, localPort);
			}
		}
	}

	private void deletePortForwarding(Session session, Integer localPort) {
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
