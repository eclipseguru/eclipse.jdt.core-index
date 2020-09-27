/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.util;

import java.util.HashMap;

/**
 * Hashtable of {char[] --> Object }
 */
public final class HashtableOfObject<V> extends HashMap<KeyOfCharArray, V>{

	/** serialVersionUID */
	private static final long serialVersionUID = 891861183675760085L;

	public HashtableOfObject() {
		this(13);
	}

	public HashtableOfObject(int size) {
		super(size);
	}

	public boolean containsKey(char[] key) {
		return super.containsKey(new KeyOfCharArray(key));
	}

	public V get(char[] key) {
		return super.get(new KeyOfCharArray(key));
	}

	public V put(char[] key, V value) {
		super.put(new KeyOfCharArray(key), value);
		return value;
	}

	public Object removeKey(char[] key) {
		return super.remove(new KeyOfCharArray(key));
	}

	@Override
	public String toString() {
		String s = ""; //$NON-NLS-1$
		for (Entry<KeyOfCharArray, V> e : super.entrySet())
			s += new String(e.getKey().toString()) + " -> " + e.getValue() + "\n"; 	//$NON-NLS-2$ //$NON-NLS-1$
		return s;
	}
}
