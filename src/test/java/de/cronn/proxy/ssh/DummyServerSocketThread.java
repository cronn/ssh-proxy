package de.cronn.proxy.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyServerSocketThread extends Thread implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(DummyServerSocketThread.class);
	private final Charset transferCharset;

	private final int port;
	private final String textToSend;
	private ServerSocket serverSocket;

	public DummyServerSocketThread(Charset transferCharset, String textToSend) throws Exception {
		super(DummyServerSocketThread.class.getSimpleName());
		this.transferCharset = transferCharset;
		this.textToSend = textToSend;
		this.serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress("localhost", 0));
		log.info("Listening on local port {}", serverSocket.getLocalPort());
		this.port = serverSocket.getLocalPort();
		setDaemon(true);
		start();
	}

	@Override
	public void close() throws IOException {
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
	}

	@Override
	public void run() {
		try (Socket socket = serverSocket.accept()) {
			log.info("got incoming connection");
			serverSocket.close();
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write((textToSend + "\r\n").getBytes(transferCharset));
			outputStream.flush();
			log.info("wrote '{}' to socket", textToSend);
		} catch (IOException e) {
			log.error("Failed to send dummy text", e);
		}
	}

	public int getPort() {
		return port;
	}
}
