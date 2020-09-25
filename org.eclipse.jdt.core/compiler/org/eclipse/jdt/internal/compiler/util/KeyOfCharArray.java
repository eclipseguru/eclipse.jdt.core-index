package org.eclipse.jdt.internal.compiler.util;

import java.util.Arrays;

/**
 * Map or Set key wrapping a char array.
 * <p>
 * The {@link #hashCode()} and {@link #equals(Object)} method will work with the underlying array using
 * <code>Arrays.hashCode</code> and <code>Arrays.equals</code>.
 * </p>
 */
public class KeyOfCharArray implements Comparable<KeyOfCharArray>{
	private final char[] key;

	public KeyOfCharArray(char[] key) {
		this.key = key;
	}

	@Override
	public int compareTo(KeyOfCharArray o) {
		return Arrays.compare(this.key, o.key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeyOfCharArray other = (KeyOfCharArray) obj;
		return Arrays.equals(this.key, other.key);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.key);
	}

	/**
	 * @return <code>Arrays.toString</code> of the underlying array
	 */
	@Override
	public String toString() {
		return Arrays.toString(this.key);
	}
}