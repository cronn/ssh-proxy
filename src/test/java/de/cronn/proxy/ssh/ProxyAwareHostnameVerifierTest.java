package de.cronn.proxy.ssh;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProxyAwareHostnameVerifierTest {

	@Mock
	private HostnameVerifier hostnameVerifier;

	@Mock
	private SSLSession session;

	@Test
	void shouldInvokeVerifierWithOriginalHost() throws Exception {
		String originalHost = "originalHost";

		ProxyAwareHostnameVerifier sut = new ProxyAwareHostnameVerifier(hostnameVerifier, originalHost);
		sut.verify("proxy", session);

		verify(hostnameVerifier).verify(eq(originalHost), eq(session));
	}

}
