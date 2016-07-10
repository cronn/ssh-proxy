package de.cronn.proxy.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyServerSocketThread extends Thread {

	private static final Logger log = LoggerFactory.getLogger(DummyServerSocketThread.class);
	private final Charset transferCharset;

	private volatile int port;
	private final String textToSend;

	public DummyServerSocketThread(Charset transferCharset, String textToSend) {
		super(DummyServerSocketThread.class.getSimpleName());
		this.transferCharset = transferCharset;
		this.textToSend = textToSend;
		setDaemon(true);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.bind(new InetSocketAddress("localhost", 0));
			log.info("Listening on local port {}", serverSocket.getLocalPort());
			port = serverSocket.getLocalPort();
			try (Socket socket = serverSocket.accept()) {
				log.info("got incoming connection");
				serverSocket.close();
				OutputStream outputStream = socket.getOutputStream();
				outputStream.write(textToSend.getBytes(transferCharset));
				outputStream.flush();
				log.info("wrote '{}' to socket", textToSend);
			}
		} catch (IOException e) {
			log.error("Server socket failed", e);
		}
	}

	public int getPort() {
		return port;
	}
}
