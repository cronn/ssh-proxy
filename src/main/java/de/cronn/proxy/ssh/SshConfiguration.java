package de.cronn.proxy.ssh;

import static de.cronn.proxy.ssh.SshProxy.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.ConfigRepository.Config;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;

import de.cronn.proxy.ssh.JSchHelper.ServerHostKeySortOrder;
import de.cronn.proxy.ssh.util.Assert;
import de.cronn.proxy.ssh.util.Utils;

public class SshConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SshConfiguration.class);

	private static final String USER_HOME = System.getProperty("user.home");

	private static final Path SSH_HOME = Paths.get(USER_HOME, ".ssh");
	protected static final Path LOCAL_SSH_CONFIG_PATH = SSH_HOME.resolve("config");
	private static final Path LOCAL_SSH_KNOWN_HOSTS_PATH = SSH_HOME.resolve("known_hosts");
	private static final Path DEFAULT_SSH_KEY_PATH = SSH_HOME.resolve("id_rsa");

	private static final String SSH_CONFIG_KEY_IDENTITY_FILE = "IdentityFile";
	private static final String SSH_CONFIG_KEY_PROXY_COMMAND = "ProxyCommand";

	private static final String USER_NAME = System.getProperty("user.name");

	protected static final int SSH_DEFAULT_PORT = 22;

	private final JSch jsch = new JSch();
	private final ConfigRepository configRepository;

	public SshConfiguration(ConfigRepository configRepository) throws JSchException {
		JSchHelper.configureGlobalSettings();

		this.configRepository = configRepository;
		jsch.setConfigRepository(this.configRepository);

		Assert.isTrue(Files.isRegularFile(LOCAL_SSH_KNOWN_HOSTS_PATH), LOCAL_SSH_KNOWN_HOSTS_PATH + " does not exist");
		jsch.setKnownHosts(LOCAL_SSH_KNOWN_HOSTS_PATH.toString());
	}

	public static SshConfiguration getConfiguration() throws IOException, JSchException {
		Assert.isTrue(Files.isRegularFile(LOCAL_SSH_CONFIG_PATH), LOCAL_SSH_CONFIG_PATH + " does not exist");
		return new SshConfiguration(OpenSSHConfig.parseFile(LOCAL_SSH_CONFIG_PATH.toString()));
	}

	private Config getHostConfig(String host) {
		return configRepository.getConfig(host);
	}

	public SshProxyCommand getProxyCommandConfiguration(String host) {
		Config config = getHostConfig(host);
		String sshProxyCommand = config.getValue(SSH_CONFIG_KEY_PROXY_COMMAND);
		if (sshProxyCommand == null) {
			return null;
		} else {
			return SshProxyCommand.parse(sshProxyCommand, host, config);
		}
	}

	void addIdentity(String host) throws JSchException {
		Config hostConfig = getHostConfig(host);
		String identityFile = hostConfig.getValue(SSH_CONFIG_KEY_IDENTITY_FILE);
		if (identityFile == null) {
			identityFile = DEFAULT_SSH_KEY_PATH.toString();
		}
		log.debug("using SSH key file {}", identityFile);
		jsch.addIdentity(identityFile);
	}

	public String getHostUser(String host) {
		Config hostConfig = getHostConfig(host);
		String hostUser = hostConfig.getUser();
		if (hostUser == null) {
			return USER_NAME;
		} else {
			return hostUser;
		}
	}

	public String getHostName(String host) {
		Config hostConfig = getHostConfig(host);
		String hostname = hostConfig.getHostname();
		if (hostname == null) {
			return host;
		} else {
			return hostname;
		}
	}

	private void configureHostKeyOrder(String host) {
		Config hostConfig = getHostConfig(host);
		ServerHostKeySortOrder hostKeySortOrder = guessPreferredHostKeySortOrder(host, hostConfig);
		JSchHelper.reconfigureServerHostKeyOrder(hostKeySortOrder);
	}

	private ServerHostKeySortOrder guessPreferredHostKeySortOrder(String jumpHostName, Config hostConfig) {
		HostKeyRepository hostKeyRepository = jsch.getHostKeyRepository();

		List<String> potentialHostNames = new ArrayList<>();
		potentialHostNames.add(jumpHostName);

		String configHostName = hostConfig.getHostname();
		if (configHostName != null) {
			if (hostConfig.getPort() > 0 && hostConfig.getPort() != SSH_DEFAULT_PORT) {
				configHostName = "[" + configHostName + "]:" + hostConfig.getPort();
			}
			potentialHostNames.add(configHostName);
		}

		for (String hostname : potentialHostNames) {
			for (HostKeyType hostKeyType : EnumSet.of(HostKeyType.ECDSA256, HostKeyType.ECDSA384, HostKeyType.ECDSA521)) {
				HostKey[] hostKeys = hostKeyRepository.getHostKey(hostname, hostKeyType.getTypeString());
				if (Utils.isNotEmpty(hostKeys)) {
					return ServerHostKeySortOrder.PREFER_ECDSA;
				}
			}

			for (HostKeyType hostKeyType : EnumSet.of(HostKeyType.SSH_RSA)) {
				HostKey[] hostKeys = hostKeyRepository.getHostKey(hostname, hostKeyType.getTypeString());
				if (Utils.isNotEmpty(hostKeys)) {
					return ServerHostKeySortOrder.PREFER_RSA;
				}
			}
		}

		String hostDescription = jumpHostName;
		if (hostConfig.getHostname() != null) {
			hostDescription += " (" + hostConfig.getHostname() + ")";
		}
		throw new IllegalArgumentException("Found no host key for " + hostDescription + " in " + hostKeyRepository.getKnownHostsRepositoryID());
	}

	public Session openSession(String host) throws JSchException {
		configureHostKeyOrder(host);
		return jsch.getSession(host);
	}

	public Session openSession(String hostUser, String jumpHost, int jumpPort) throws JSchException {
		configureHostKeyOrder(jumpHost);
		return jsch.getSession(hostUser, LOCALHOST, jumpPort);
	}
}
