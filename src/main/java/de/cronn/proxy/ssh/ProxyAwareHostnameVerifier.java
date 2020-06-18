package de.cronn.proxy.ssh;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class ProxyAwareHostnameVerifier implements HostnameVerifier {

	private final HostnameVerifier hostnameVerifier;
	private final String originalHost;

	public ProxyAwareHostnameVerifier(HostnameVerifier hostnameVerifier, String originalHost) {
		this.hostnameVerifier = hostnameVerifier;
		this.originalHost = originalHost;
	}

	@Override
	public boolean verify(String host, SSLSession sslSession) {
		return hostnameVerifier.verify(originalHost, sslSession);
	}

}
