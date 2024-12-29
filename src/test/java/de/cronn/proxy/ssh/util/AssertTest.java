package de.cronn.proxy.ssh.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class AssertTest {

	@Test
	void testConstructorIsPrivate() throws Exception {
		Constructor<?> constructor = Assert.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	void testNotNull() {
		Assert.notNull("", "should not be null");
		Assert.notNull(new Object(), "should not be null");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.notNull(null, "should not be null"))
			.withMessage("should not be null");
	}

	@Test
	void testIsNull() {
		Assert.isNull(null, "should be null");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.isNull("", "should not be null"))
			.withMessage("should not be null");
	}

	@Test
	void testIsTrue() {
		Assert.isTrue(true, "should be true");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> Assert.isTrue(false, "should not be true"))
			.withMessage("should not be true");
	}

}
