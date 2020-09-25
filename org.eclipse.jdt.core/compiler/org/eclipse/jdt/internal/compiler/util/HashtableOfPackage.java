/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import static java.util.stream.Collectors.joining;

import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;

public final class HashtableOfPackage<P extends PackageBinding> {

	private final HashMap<KeyOfCharArray, P> map;

	public HashtableOfPackage() {
		this(3); // usually not very large
	}

	public HashtableOfPackage(int size) {
		this.map = new HashMap<>(size);
	}

	public Iterable<P> values() {
		return this.map.values();
	}

	public boolean containsKey(char[] key) {
		return this.map.containsKey(new KeyOfCharArray(key));
	}

	public P get(char[] key) {
		return this.map.get(new KeyOfCharArray(key));
	}

	public P put(char[] key, P value) {
		return this.map.put(new KeyOfCharArray(key), value);
	}

	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return this.map.values().stream().map(P::toString).collect(joining("\n")); //$NON-NLS-1$
	}
}
