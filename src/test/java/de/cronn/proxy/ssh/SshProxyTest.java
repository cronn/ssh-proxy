package de.cronn.proxy.ssh;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.hamcrest.CoreMatchers;
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
	private static final Path SERVER_RSA_KEY = TEST_RESOURCES.resolve("server-rsa.key");
	private static final Path SERVER_ECDSA_KEY = TEST_RESOURCES.resolve("server-ecdsa.key");

	private static final long TEST_TIMEOUT_MILLIS = 30_000L;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private String oldUserHome;
	private Path dotSsh;

	private static final String TEST_TEXT = "Hello World";

	@Before
	public void checkBouncyCastleIsRegistered() {
		assertTrue("BouncyCastle is registered", SecurityUtils.isBouncyCastleRegistered());
	}

	@Before
	public void setUp() throws Exception {
		Path userHome = temporaryFolder.getRoot().toPath();
		oldUserHome = System.getProperty("user.home");
		System.setProperty("user.home", userHome.toAbsolutePath().toString());
		log.debug("changed 'user.home' to {}", System.getProperty("user.home"));

		dotSsh = userHome.resolve(".ssh");
		Files.createDirectories(dotSsh);

		for (String file : Arrays.asList("id_rsa", "id_rsa.pub")) {
			Files.copy(TEST_RESOURCES.resolve(file), dotSsh.resolve(file));
		}

		appendToSshFile(CONFIG_FILENAME, "");
		appendToSshFile(KNOWN_HOSTS_FILENAME, "");
	}

	@After
	public void tearDown() {
		System.setProperty("user.home", oldUserHome);
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop() throws Exception {
		SshServer sshServer = setUpSshServer();
		int sshServerPort = sshServer.getPort();

		String hostConfigName = "localhost-" + sshServerPort;
		appendToSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
			 SshProxy sshProxy = new SshProxy()) {
			int port = sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort());

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readLine(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(sshServer);
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHopWithPassphraseAuthentication() throws Exception {
		setupNewSshKeys("id_rsa_passphrase", "id_rsa_passphrase.pub");
		SshServer sshServer = setUpSshServer();
		int sshServerPort = sshServer.getPort();

		String hostConfigName = "localhost-" + sshServerPort;
		appendToSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		String testPassword = "password";

		try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
			 SshProxy sshProxy = new SshProxy(testPassword)) {
			int port = sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort());

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readLine(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(sshServer);
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHopWithPassphraseAuthentication_IncorrectPassphrase() throws Exception {
		setupNewSshKeys("id_rsa_passphrase", "id_rsa_passphrase.pub");
		SshServer sshServer = setUpSshServer();
		int sshServerPort = sshServer.getPort();

		String hostConfigName = "localhost-" + sshServerPort;
		appendToSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		String testPassword = "incorrectPassword";

		assertThrows(de.cronn.proxy.ssh.SshProxyRuntimeException.class, () -> {
			try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
				 SshProxy sshProxy = new SshProxy(testPassword)) {
				sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort());
			}
		});
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop_EcDsaServer() throws Exception {
		SshServer sshServer = setUpSshServer(KeyUtils.EC_ALGORITHM);
		int sshServerPort = sshServer.getPort();

		String hostConfigName = "localhost-" + sshServerPort;
		appendToSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
			 SshProxy sshProxy = new SshProxy()) {
			int port = sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort());

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readLine(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(sshServer);
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHopWithLocalPort() throws Exception {
		SshServer sshServer = setUpSshServer();
		int sshServerPort = sshServer.getPort();

		String hostConfigName = "localhost-" + sshServerPort;
		appendToSshFile(CONFIG_FILENAME, "Host " + hostConfigName + "\n\tHostName localhost\n\tPort " + sshServerPort + "\n\n");

		try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
			 SshProxy sshProxy = new SshProxy()) {
			int port = sshProxy.connect(hostConfigName, "localhost", dummyServerSocketThread.getPort(), 2345);

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readLine(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(sshServer);
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testTwoHops_ProxyCommand() throws Exception {
		doTestTwoHops("ProxyCommand ssh -q -W %h:%p firsthop");
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testTwoHops_ProxyJump() throws Exception {
		doTestTwoHops("ProxyJump firsthop");
	}

	private void doTestTwoHops(String proxyConfiguration) throws Exception {
		SshServer firstSshServer = setUpSshServer();
		int firstServerPort = firstSshServer.getPort();

		SshServer secondSshServer = setUpSshServer();
		int secondServerPort = secondSshServer.getPort();

		appendToSshFile(CONFIG_FILENAME, "Host firsthop\n\tHostName localhost\n\tPort " + firstServerPort + "\n\n");
		appendToSshFile(CONFIG_FILENAME, "Host secondhop\n\tHostName localhost\n\tPort " + secondServerPort + "\n\t" + proxyConfiguration + "\n\n");

		try (DummyServerSocketThread dummyServerSocketThread = new DummyServerSocketThread(TRANSFER_CHARSET, TEST_TEXT);
			 SshProxy sshProxy = new SshProxy()) {
			int port = sshProxy.connect("secondhop", "localhost", dummyServerSocketThread.getPort());

			final String receivedText;
			try (Socket s = new Socket(SshProxy.LOCALHOST, port);
				 InputStream is = s.getInputStream()) {
				log.info("connected to port: {}", port);
				receivedText = readLine(is);
			}
			assertEquals(TEST_TEXT, receivedText);
		} finally {
			tryStop(firstSshServer);
			tryStop(secondSshServer);
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop_NoHostKeyFound() throws Exception {
		try (SshProxy sshProxy = new SshProxy()) {
			sshProxy.connect("jumphost", "targethost", 1234);
			fail("SshProxyRuntimeException expected");
		} catch (SshProxyRuntimeException e) {
			log.debug("Expected exception", e);
			assertEquals("Failed to create SSH tunnel to targethost via jumphost", e.getMessage());
			assertThat(e.getCause().getMessage(), CoreMatchers.startsWith("Found no host key for jumphost"));
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop_ConnectionRefused() throws Exception {
		SshServer sshServer = setUpSshServer();
		sshServer.stop();
		try (SshProxy sshProxy = new SshProxy()) {
			sshProxy.connect("localhost", "targethost", 1234);
			fail("SshProxyRuntimeException expected");
		} catch (SshProxyRuntimeException e) {
			log.debug("Expected exception", e);
			assertEquals("Failed to create SSH tunnel to targethost via localhost", e.getMessage());
			assertEquals("Failed to connect to targethost via localhost", e.getCause().getMessage());
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop_IllegalPort() throws Exception {
		try (SshProxy sshProxy = new SshProxy()) {
			sshProxy.connect("localhost", "targethost", 0);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("illegal port: 0", e.getMessage());
		}
	}

	@Test(timeout = TEST_TIMEOUT_MILLIS)
	public void testSingleHop_IllegalLocalPort() throws Exception {
		try (SshProxy sshProxy = new SshProxy()) {
			sshProxy.connect("localhost", "targethost", 1234, -1);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("illegal local port: -1", e.getMessage());
		}
	}

	private void tryStop(SshServer sshServer) {
		try {
			log.debug("stopping SSH server");
			sshServer.stop();
		} catch (IOException e) {
			log.error("Failed to stop SSH server", e);
		}
	}

	private String readLine(InputStream is) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, TRANSFER_CHARSET))) {
			String line = reader.readLine();
			assertNotNull(line);
			return line.trim();
		}
	}

	private SshServer setUpSshServer() throws IOException {
		return setUpSshServer(KeyUtils.RSA_ALGORITHM);
	}

	private SshServer setUpSshServer(String algorithm) throws IOException {
		SshServer sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(0);
		AbstractGeneratorHostKeyProvider hostKeyProvider = SecurityUtils.createGeneratorHostKeyProvider(getServerKeyFile(algorithm));
		hostKeyProvider.setAlgorithm(algorithm);
		if (algorithm.equals(KeyUtils.EC_ALGORITHM)) {
			hostKeyProvider.setKeySize(256);
		}
		sshServer.setKeyPairProvider(hostKeyProvider);

		sshServer.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
		sshServer.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);

		writeFingerprintToKnownHosts(algorithm);

		sshServer.start();

		int sshServerPort = sshServer.getPort();
		assertTrue(sshServerPort > 0);

		return sshServer;
	}

	private void writeFingerprintToKnownHosts(String algorithm) throws IOException {
		switch (algorithm) {
			case KeyUtils.RSA_ALGORITHM:
				appendToSshFile(KNOWN_HOSTS_FILENAME, "localhost ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDL8360Wxcgo33sggS0bSid0u7Ad4XFig8/e0UfD5l02x/w2DRJuqJow4SiDfi9jvD8p3lu7To7b/oGH/c/vsK9j35ICG0eJ/bbnQDuHROBAnbAC6PXN+/XX2F9s48KlOC5dQXrGYyYhoozW67yoHTooisZSzF/iyPdNat64rM0+ZO3dV6eEQ0FItYO632YcSiBRE7YZe9rP7ne50xaltKgrAmHRDRo+tjIcykrlcZFG1Bp/ct9Ejs2DQDsFOZRCmFbag0pQxxbkA1U6z7O3qwhhDWcJz2ZHDHK8DUkgHdX+Hbp7LxBWEaCiU8cL+S6rmCpNsui9NT/XeoLuXQ4J8jX\n");
				break;
			case KeyUtils.EC_ALGORITHM:
				appendToSshFile(KNOWN_HOSTS_FILENAME, "localhost ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBCH+0xjLYNGoqVGlD4VtKHF1Tig2/Y76BxVld88bYAaRV4ojJni62vIYMKqk+FMZhL1lcQ/VQTvIeLMnYk+grKo=\n");
				break;
			default:
				throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
		}
	}

	private static Path getServerKeyFile(String algorithm) {
		switch (algorithm) {
			case KeyUtils.RSA_ALGORITHM:
				return SERVER_RSA_KEY;
			case KeyUtils.EC_ALGORITHM:
				return SERVER_ECDSA_KEY;
			default:
				throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
		}
	}

	private void appendToSshFile(String filename, String text) throws IOException {
		Path config = dotSsh.resolve(filename);
		Files.write(config, text.getBytes(CONFIG_CHARSET), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
	}

	private void setupNewSshKeys(String privateKeyFile, String publicKeyFile) throws IOException {
		Path dotSsh = Paths.get(System.getProperty("user.home"), ".ssh");
		Path TEST_RESOURCES = Paths.get("src", "test", "resources");
		Files.copy(TEST_RESOURCES.resolve(privateKeyFile), dotSsh.resolve("id_rsa"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(TEST_RESOURCES.resolve(publicKeyFile), dotSsh.resolve("id_rsa.pub"), StandardCopyOption.REPLACE_EXISTING);
	}

}
