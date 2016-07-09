package de.cronn.proxy.ssh.util;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class AssertTest {

	@Test
	public void testConstructorIsPrivate() throws Exception {
		Constructor<?> constructor = Assert.class.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void testNotNull() throws Exception {
		Assert.notNull("", "should not be null");
		Assert.notNull(new Object(), "should not be null");

		try {
			Assert.notNull(null, "should not be null");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("should not be null", e.getMessage());
		}
	}

	@Test
	public void testIsNull() throws Exception {
		Assert.isNull(null, "should be null");

		try {
			Assert.isNull("", "should not null");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("should not null", e.getMessage());
		}
	}

	@Test
	public void testIsTrue() throws Exception {
		Assert.isTrue(true, "should be true");

		try {
			Assert.isTrue(false, "should not true");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertEquals("should not true", e.getMessage());
		}
	}

}