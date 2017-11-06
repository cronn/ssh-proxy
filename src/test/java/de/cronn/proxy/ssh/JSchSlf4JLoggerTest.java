package de.cronn.proxy.ssh;

import static org.junit.Assert.*;

import org.junit.Test;

import com.jcraft.jsch.Logger;

public class JSchSlf4JLoggerTest {

	@Test
	public void testIsEnabled() throws Exception {
		JSchSlf4JLogger logger = new JSchSlf4JLogger();

		assertFalse(logger.isEnabled(Logger.DEBUG));
		assertFalse(logger.isEnabled(Logger.INFO));
		assertTrue(logger.isEnabled(Logger.WARN));
		assertTrue(logger.isEnabled(Logger.ERROR));
		assertTrue(logger.isEnabled(Logger.FATAL));
	}

	@Test
	public void testLog() throws Exception {
		JSchSlf4JLogger logger = new JSchSlf4JLogger();

		logger.log(Logger.DEBUG, "some debug message");
		logger.log(Logger.INFO, "some info message");
		logger.log(Logger.WARN, "some warning message");
		logger.log(Logger.ERROR, "some error message");
		logger.log(Logger.FATAL, "some fatal message");
	}

}