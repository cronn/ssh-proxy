package de.cronn.proxy.ssh;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshProxyTest {

	private static final Logger log = LoggerFactory.getLogger(SshProxyTest.class);

	private static final Charset CONFIG_CHARSET = StandardCharsets.ISO_8859_1;
	private static final Charset TRANSFER_CHARSET = StandardCharsets.UTF_16;
	private static final String KNOWN_HOSTS_FILENAME = "known_hosts";
	private static final String CONFIG_FILENAME = "config";
	private static final Path TEST_RESOURCES = Paths.get("src", "test", "resources");
	private static final Path SERVER_HOST_KEY = TEST_RESOURCES.resolve("server-host.key");

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private String oldUserHome;
	private Path dotSsh;

	private static final String TEST_TEXT = "Hello World";

	@Before
	public void setUp() throws Exception {
		Path userHome = temporaryFolder.newFolder().toPath();
		oldUserHome = System.getProperty("user.home");
		System.setProperty("user.home", userHome.toAbsolutePath().toString());
		log.debug("changed 'user.home' to {}", System.getProperty("user.home"));

		dotSsh = userHome.resolve(".ssh");
		Files.createDirectories(dotSsh);

		for (String file : Arrays.asList("id_rsa", "id_rsa.pub")) {
			Files.copy(TEST_RESOURCES.resolve(file), dotSsh.resolve(file));
		}
	}

	@After
	public void tearDown() {
		System.setProperty("user.home", oldUserHome);
	}

	@Test
	public void testSingleHop() throws Exception {
		SshServer sshServer = setUpSshServer();
		int sshServerPort = sshServer.getPort();
		assertTrue(sshServerPort > 0);

		String hostConfigName = "localhost-" + sshServerPort;
		writeSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
		dummyServerSocketThread.start();

		try (SshProxy sshProxy = new SshProxy()) {
			int port = sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort());

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readText(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(sshServer);
		}
	}

	private void tryStop(SshServer sshServer) {
		try {
			sshServer.stop();
		} catch (IOException e) {
			log.error("Failed to stop SSH server", e);
		}
	}

	private String readText(InputStream is) throws IOException {
		byte[] buffer = new byte[256];
		int numRead = is.read(buffer);
		return new String(buffer, 0, numRead, TRANSFER_CHARSET);
	}

	private SshServer setUpSshServer() throws IOException {
		SshServer sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(0);
		SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider(SERVER_HOST_KEY);
		keyPairProvider.setAlgorithm(KeyUtils.RSA_ALGORITHM);
		sshServer.setKeyPairProvider(keyPairProvider);

		sshServer.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
		sshServer.setTcpipForwardingFilter(AcceptAllForwardingFilter.INSTANCE);

		sshServer.start();

		int sshServerPort = sshServer.getPort();

		writeSshFile(KNOWN_HOSTS_FILENAME, "[localhost]:" + sshServerPort + " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC7inSl9nmddrnG8edpFzocy3fDzLOkUCHFjVSu/4XTOsGGz2U3YpxhHqORsv7EjWydHO6d1kmkca77iIakAqHp8P9/owgqhFCQYNIxzebgD0HBt/Jw2Lx857jYjlJhbbsZVLGJpyZASK88Tr6jqQ4P66HO+vHwkTfz3XVmgt70u65bXKZpjeu7hiqvAvvAthdnK+WR5EaA9SmpTWx4+XPMac7PEePOwQszBoUdkNh/S/iDS/W/CnKs9TIjjGuOouDc5LSE0BiD736PoVJUL/JsI811BkfJ9T26WshghGplJpKVSKYlF/qPf4AvYz22IPKCsZ0tGOUt8bBVG8DmZyZP\n");

		return sshServer;
	}

	private void writeSshFile(String filename, String text) throws IOException {
		Path config = dotSsh.resolve(filename);
		Files.write(config, text.getBytes(CONFIG_CHARSET));
	}

}
