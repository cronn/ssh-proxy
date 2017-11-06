package de.cronn.proxy.ssh.util;

import java.util.List;

public final class Utils {

	private Utils() {
	}

	public static String join(List<String> strings, String separator) {
		if (strings == null || strings.isEmpty()) {
			return "";
		}

		StringBuilder b = new StringBuilder();
		for (int i = 0; ; i++) {
			b.append(strings.get(i));
			if (i == strings.size() - 1) {
				return b.toString();
			}
			b.append(separator);
		}
	}


	public static boolean isNotEmpty(Object[] array) {
		return array != null && array.length > 0;
	}
}
