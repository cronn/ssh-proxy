package de.cronn.proxy.ssh;


import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.jcraft.jsch.Logger;

class JSchSlf4JLoggerTest {

	@Test
	void testIsEnabled() {
		JSchSlf4JLogger logger = new JSchSlf4JLogger();

		assertThat(logger.isEnabled(Logger.DEBUG)).isFalse();
		assertThat(logger.isEnabled(Logger.INFO)).isFalse();
		assertThat(logger.isEnabled(Logger.WARN)).isTrue();
		assertThat(logger.isEnabled(Logger.ERROR)).isTrue();
		assertThat(logger.isEnabled(Logger.FATAL)).isTrue();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> logger.isEnabled(100))
			.withMessage("Unknown log level: 100");
	}

	@Test
	void testLog() {
		JSchSlf4JLogger logger = new JSchSlf4JLogger();

		logger.log(Logger.DEBUG, "some debug message");
		logger.log(Logger.INFO, "some info message");
		logger.log(Logger.WARN, "some warning message");
		logger.log(Logger.ERROR, "some error message");
		logger.log(Logger.FATAL, "some fatal message");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> logger.log(100, "some message"))
			.withMessage("Unknown log level: 100");
	}

}
