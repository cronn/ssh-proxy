package de.cronn.proxy.ssh;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyAwareHostnameVerifierTest {

	@Test
	public void shouldInvokeVerifierWithOriginalHost() throws Exception {
		HostnameVerifier hostnameVerifier = mock(HostnameVerifier.class);
		String originalHost = "originalHost";
		SSLSession session = mock(SSLSession.class);

		ProxyAwareHostnameVerifier sut = new ProxyAwareHostnameVerifier(hostnameVerifier, originalHost);
		sut.verify("proxy", session);

		verify(hostnameVerifier).verify(eq(originalHost), eq(session));
	}

}
