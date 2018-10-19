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

public final class SolrConstantEnum extends SolrConstant {

	private static final String ENUM_VALUE = "enumValue"; //$NON-NLS-1$
	private static final String ENUM_TYPE = "enumType"; //$NON-NLS-1$
	
	private static final String TYPE_CONSTANTENUM = "constantenum"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_CONSTANTENUM; //$NON-NLS-1$
	
	public SolrConstantEnum(String enumType, String enumValue) {
		super(TYPE_CONSTANTENUM);
		setEnumType(enumType);
		setEnumValue(enumValue);
	}

	public void setEnumValue(String enumValue) {
		this.document.setField(ENUM_VALUE, enumValue);
	}

	public void setEnumType(String enumType) {
		this.document.setField(ENUM_TYPE, enumType);
	}

	@Override
	public Constant getConstant() {
		return null;
	}
}
