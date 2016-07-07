package de.cronn.proxy.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;

import de.cronn.proxy.ssh.util.Utils;

public final class JSchHelper {

	private static final Logger log = LoggerFactory.getLogger(JSchHelper.class);

	private static final String SERVER_HOST_KEY_SEPARATOR = ",";

	private static final String JSCH_CONFIG_KEY_SERVER_HOST_KEY = "server_host_key";
	private static final String JSCH_CONFIG_KEY_PREFERRED_AUTHENTICATIONS = "PreferredAuthentications";

	private static class HostKeyComparator implements Comparator<HostKeyType> {

		private final List<HostKeyType> sortOrder;

		protected HostKeyComparator(List<HostKeyType> sortOrder) {
			this.sortOrder = sortOrder;
		}

		protected HostKeyComparator(HostKeyType... sortOrder) {
			this(Arrays.asList(sortOrder));
		}

		@Override
		public int compare(HostKeyType a, HostKeyType b) {
			int indexA = sortOrder.indexOf(a);
			int indexB = sortOrder.indexOf(b);
			return Integer.compare(indexA, indexB);
		}
	}

	private static final Comparator<HostKeyType> CMP_PREFER_ECDSA = new HostKeyComparator(
		HostKeyType.ECDSA256, HostKeyType.ECDSA384, HostKeyType.ECDSA521, HostKeyType.SSH_RSA, HostKeyType.SSH_DSS
	);

	private static final Comparator<HostKeyType> CMP_PREFER_RSA = new HostKeyComparator(
		HostKeyType.SSH_RSA, HostKeyType.ECDSA256, HostKeyType.ECDSA384, HostKeyType.ECDSA521, HostKeyType.SSH_DSS
	);

	public enum ServerHostKeySortOrder {
		PREFER_ECDSA,
		PREFER_RSA,
	}

	private JSchHelper() {
	}

	protected static void reconfigureServerHostKeyOrder(ServerHostKeySortOrder hostKeySortOrder) {
		List<HostKeyType> serverHostKeys = new ArrayList<>(getServerHostKeys());
		if (hostKeySortOrder == ServerHostKeySortOrder.PREFER_ECDSA) {
			Collections.sort(serverHostKeys, CMP_PREFER_ECDSA);
		} else if (hostKeySortOrder == ServerHostKeySortOrder.PREFER_RSA) {
			Collections.sort(serverHostKeys, CMP_PREFER_RSA);
		} else {
			throw new IllegalArgumentException("Unknown host key sort order: " + hostKeySortOrder);
		}

		if (!getServerHostKeys().equals(serverHostKeys)) {
			log.debug("changing server host key order to: " + serverHostKeys);

			List<String> serverHostKeyNames = new ArrayList<>();
			for (HostKeyType serverHostKey : serverHostKeys) {
				serverHostKeyNames.add(serverHostKey.getTypeString());
			}

			String newHostKeyOrder = Utils.join(serverHostKeyNames, SERVER_HOST_KEY_SEPARATOR);
			JSch.setConfig(JSCH_CONFIG_KEY_SERVER_HOST_KEY, newHostKeyOrder);
		}
	}

	protected static void reconfigurePreferredAuthentications() {
		JSch.setConfig(JSCH_CONFIG_KEY_PREFERRED_AUTHENTICATIONS, "publickey");
	}

	protected static void registerLogger() {
		JSch.setLogger(new JSchSlf4JLogger());
	}

	protected static List<HostKeyType> getServerHostKeys() {
		String serverHostKey = JSch.getConfig(JSCH_CONFIG_KEY_SERVER_HOST_KEY);

		List<HostKeyType> hostKeyTypes = new ArrayList<>();
		for (String hostKeyString : Arrays.asList(serverHostKey.split(SERVER_HOST_KEY_SEPARATOR))) {
			hostKeyTypes.add(HostKeyType.byTypeString(hostKeyString));
		}
		return hostKeyTypes;
	}

	public static void configureGlobalSettings() {
		reconfigurePreferredAuthentications();
		registerLogger();
	}
}
