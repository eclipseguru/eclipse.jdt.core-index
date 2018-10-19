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

public final class SolrConstantClass extends SolrConstant {
	
	private static final String TYPE_ID = "typeId"; //$NON-NLS-1$
	
	private static final String TYPE_CONSTANTCLASS = "constantclass"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_CONSTANTCLASS; //$NON-NLS-1$


	public SolrConstantClass(String typeId) {
		super(TYPE_CONSTANTCLASS);
		setTypeId(typeId);
	}

	public void setTypeId(String typeId) {
		this.document.setField(TYPE_ID, typeId);
	}


	@Override
	public Constant getConstant() {
		return null;
	}
}
