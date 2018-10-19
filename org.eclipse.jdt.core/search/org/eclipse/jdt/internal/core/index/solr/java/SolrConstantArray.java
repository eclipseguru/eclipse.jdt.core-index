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

import java.util.List;

import org.eclipse.jdt.internal.compiler.impl.Constant;

public final class SolrConstantArray extends SolrConstant {

	private static final String TYPE_CONSTANTARRAY = "constantannotation"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_CONSTANTARRAY; //$NON-NLS-1$
	private List<SolrConstant> elements;
	
	public SolrConstantArray() {
		super(TYPE_CONSTANTARRAY);
	}

	@Override
	public Constant getConstant() {
		return null;
	}

	public List<SolrConstant> getElements() {
		return this.elements;
	}
	
	public void setValue(List<SolrConstant> elements) {
		this.elements = elements;
		if(elements != null) {
			for (SolrConstant constant : elements) {
				constant.setParent(this);
			}
		}
	}
}
