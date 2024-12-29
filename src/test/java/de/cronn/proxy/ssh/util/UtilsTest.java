package de.cronn.proxy.ssh.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class UtilsTest {

	@Test
	void testConstructorIsPrivate() throws Exception {
		Constructor<?> constructor = Utils.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	void testJoin() throws Exception {
		assertThat(Utils.join(null, "")).isEqualTo("");
		assertThat(Utils.join(Collections.emptyList(), "")).isEqualTo("");
		assertThat(Utils.join(Collections.singletonList("foo"), ";")).isEqualTo("foo");
		assertThat(Utils.join(Arrays.asList("foo", "bar"), ";")).isEqualTo("foo;bar");
	}

	@Test
	void testIsNotEmpty() throws Exception {
		assertThat(Utils.isNotEmpty(null)).isFalse();
		assertThat(Utils.isNotEmpty(new Object[0])).isFalse();
		assertThat(Utils.isNotEmpty(new Object[] { null })).isTrue();
		assertThat(Utils.isNotEmpty(new Object[] { "foo", "bar" })).isTrue();
	}

}
