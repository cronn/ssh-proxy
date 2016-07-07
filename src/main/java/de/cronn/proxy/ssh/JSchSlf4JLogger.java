package de.cronn.proxy.ssh;

import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Logger;

public class JSchSlf4JLogger implements Logger {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JSchSlf4JLogger.class);

	@Override
	public boolean isEnabled(int level) {
		switch (level) {
			case DEBUG:
				return logger.isTraceEnabled();
			case INFO:
				return logger.isTraceEnabled();
			case WARN:
				return logger.isWarnEnabled();
			case ERROR:
			case FATAL:
				return logger.isErrorEnabled();
			default:
				throw new IllegalArgumentException("Unknown log level: " + level);
		}
	}

	@Override
	public void log(int level, String message) {
		switch (level) {
			case DEBUG:
				logger.trace(message);
				break;
			case INFO:
				logger.trace(message);
				break;
			case WARN:
				logger.warn(message);
				break;
			case ERROR:
				logger.error(message);
				break;
			case FATAL:
				logger.error("FATAL: {}", message);
				break;
			default:
				throw new IllegalArgumentException("Unknown log level: " + level);
		}
	}
}
