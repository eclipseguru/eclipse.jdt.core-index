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
package org.eclipse.jdt.internal.compiler.util;

import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

public final class HashtableOfType extends HashMap<KeyOfCharArray, ReferenceBinding>{

	/** serialVersionUID */
	private static final long serialVersionUID = 9128824057496562784L;

public HashtableOfType() {
	this(3);
}
public HashtableOfType(int size) {
	super(size);
}
public boolean containsKey(char[] key) {
	return super.containsKey(new KeyOfCharArray(key));
}
public ReferenceBinding get(char[] key) {
	return super.get(new KeyOfCharArray(key));
}
// Returns old value.
public ReferenceBinding getput(char[] key, ReferenceBinding value) {
	return super.put(new KeyOfCharArray(key), value);
}
//Returns new value.
public ReferenceBinding put(char[] key, ReferenceBinding value) {
    super.put(new KeyOfCharArray(key), value);
	return value;
}
@Override
public String toString() {
	String s = ""; //$NON-NLS-1$
	for (ReferenceBinding type : values()) {
			s += type.toString() + "\n"; //$NON-NLS-1$
	}
	return s;
}
}
