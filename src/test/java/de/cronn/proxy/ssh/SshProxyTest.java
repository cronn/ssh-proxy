package de.cronn.proxy.ssh;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshProxyTest {

	private static final Logger log = LoggerFactory.getLogger(SshProxyTest.class);

	private SshProxy sshProxy;

	@Before
	public void setUp() {
		Assume.assumeTrue(Boolean.getBoolean("de.cronn.proxy.ssh.integration.test"));
		sshProxy = new SshProxy();
	}

	@After
	public void tearDown() {
		if (sshProxy != null) {
			sshProxy.close();
		}
	}

	private void connect(int port) throws Exception {
		try (Socket s = new Socket(SshProxy.LOCALHOST, port);
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream()) {
			log.info("connected to port: {}", port);
			is.close();
			os.close();
		}
	}

	@Test
	public void testSingleHop() throws Exception {
		int port = sshProxy.connect("jumphost", "localhost", 22);
		connect(port);
	}

}
