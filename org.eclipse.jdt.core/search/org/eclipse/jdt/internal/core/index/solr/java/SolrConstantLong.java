/*******************************************************************************
 * Copyright (c) 2015, 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.index.solr.java;

import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.LongConstant;

public final class SolrConstantLong extends SolrConstantWithValue<Long> {
	
	private static final String TYPE_CONSTANTLONG = "constantlong"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_CONSTANTLONG; //$NON-NLS-1$

	public SolrConstantLong(long value) {
		super(TYPE_CONSTANTLONG, 0L);
		setValue(value);
	}

	@Override
	public Constant getConstant() {
		return LongConstant.fromValue(getValue());
	}
}
