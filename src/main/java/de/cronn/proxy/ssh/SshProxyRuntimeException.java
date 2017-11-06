package de.cronn.proxy.ssh;

public class SshProxyRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SshProxyRuntimeException(String message) {
		super(message);
	}

	public SshProxyRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
