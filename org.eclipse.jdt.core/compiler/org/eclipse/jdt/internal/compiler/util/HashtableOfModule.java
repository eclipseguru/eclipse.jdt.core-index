/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;

public final class HashtableOfModule extends HashMap<KeyOfCharArray, ModuleBinding>{

	/** serialVersionUID */
	private static final long serialVersionUID = 1480417895523082781L;

	public HashtableOfModule() {
		this(3); // usually not very large
	}
	public HashtableOfModule(int size) {
		super(size);
	}
	public boolean containsKey(char[] key) {
		return super.containsKey(new KeyOfCharArray(key));
	}
	public ModuleBinding get(char[] key) {
		return super.get(new KeyOfCharArray(key));
	}
	public ModuleBinding put(char[] key, ModuleBinding value) {
		super.put(new KeyOfCharArray(key), value);
		return value;
	}

	@Override
	public String toString() {
		String s = ""; //$NON-NLS-1$
		for (ModuleBinding pkg : values()) {
			s += pkg.toString() + "\n"; //$NON-NLS-1$
		}
		return s;
	}
}
