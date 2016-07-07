package de.cronn.proxy.ssh.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testJoin() throws Exception {
		assertEquals("", Utils.join(null, ""));
		assertEquals("", Utils.join(Collections.<String>emptyList(), ""));
		assertEquals("foo", Utils.join(Collections.singletonList("foo"), ";"));
		assertEquals("foo;bar", Utils.join(Arrays.asList("foo", "bar"), ";"));
	}

	@Test
	public void testIsNotEmpty() throws Exception {
		assertFalse(Utils.isNotEmpty(null));
		assertFalse(Utils.isNotEmpty(new Object[0]));
		assertTrue(Utils.isNotEmpty(new Object[] { null }));
		assertTrue(Utils.isNotEmpty(new Object[] { "foo", "bar" }));
	}

}