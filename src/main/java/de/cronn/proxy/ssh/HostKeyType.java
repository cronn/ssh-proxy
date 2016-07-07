package de.cronn.proxy.ssh;

import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.HostKey;

import de.cronn.proxy.ssh.util.Assert;

public enum HostKeyType {

	SSH_DSS(HostKey.SSHDSS, "ssh-dss"), //
	SSH_RSA(HostKey.SSHRSA, "ssh-rsa"), //
	ECDSA256(HostKey.ECDSA256, "ecdsa-sha2-nistp256"), //
	ECDSA384(HostKey.ECDSA384, "ecdsa-sha2-nistp384"), //
	ECDSA521(HostKey.ECDSA521, "ecdsa-sha2-nistp521"), //
	;

	private final int type;
	private final String typeString;

	private static final Map<String, HostKeyType> valuesByTypeString = new HashMap<>();

	static {
		for (HostKeyType hostKeyType : HostKeyType.values()) {
			HostKeyType oldValue = valuesByTypeString.put(hostKeyType.getTypeString(), hostKeyType);
			Assert.isNull(oldValue, "Duplicate value for " + hostKeyType.getTypeString());
		}
	}

	HostKeyType(int type, String typeString) {
		this.type = type;
		this.typeString = typeString;
	}

	public int getType() {
		return type;
	}

	public String getTypeString() {
		return typeString;
	}

	public static HostKeyType byTypeString(String typeString) {
		HostKeyType hostKeyType = valuesByTypeString.get(typeString);
		Assert.notNull(hostKeyType, "No hostKeyType found for " + typeString);
		return hostKeyType;
	}
}
