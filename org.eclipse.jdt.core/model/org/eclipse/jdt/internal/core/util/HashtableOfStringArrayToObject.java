/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * {@link HashMap} of {String[] --> Object }
 */
public final class HashtableOfStringArrayToObject<V> implements Cloneable {

	public static class KeyOfStringArray implements Comparable<KeyOfStringArray> {
		private final String[] key;

		public KeyOfStringArray(String[] key) {
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyOfStringArray other = (KeyOfStringArray) obj;
			return Arrays.equals(this.key, other.key);
		}

		public String[] getObjectArray() {
			return this.key;
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

		@Override
		public int compareTo(KeyOfStringArray o) {
			return Arrays.compare(this.key, o.key);
		}
	}

	private HashMap<KeyOfStringArray, V> map;

	public HashtableOfStringArrayToObject() {
		this(13);
	}

	public HashtableOfStringArrayToObject(int size) {
		this.map = new HashMap<>(size);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		HashtableOfStringArrayToObject<V> result;
		try {
			result = (HashtableOfStringArrayToObject<V>) super.clone();
			result.map = (HashMap<KeyOfStringArray, V>) this.map.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we and HashMap is Cloneable
			throw new InternalError(e);
		}
		return result;
	}

	public boolean containsKey(String[] key) {
		return this.map.containsKey(new KeyOfStringArray(key));
	}

	public Set<Entry<KeyOfStringArray, V>> entrySet() {
		return this.map.entrySet();
	}

	public V get(KeyOfStringArray key) {
		return this.map.get(key);
	}

	public V get(String[] key) {
		return this.map.get(new KeyOfStringArray(key));
	}

	public Set<KeyOfStringArray> keySet() {
		return this.map.keySet();
	}

	public V put(String[] key, V value) {
		return this.map.put(new KeyOfStringArray(key), value);
	}

	public Object removeKey(String[] key) {
		return this.map.remove(new KeyOfStringArray(key));
	}

	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return this.map.entrySet().stream().map(e -> e.getKey().toString() + " -> " + e.getValue()) //$NON-NLS-1$
				.collect(joining("\n")); //$NON-NLS-1$
	}

	public Collection<V> values() {
		return this.map.values();
	}
}
