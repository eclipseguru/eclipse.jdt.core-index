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

public final class SolrConstantAnnotation extends SolrConstant {

	private static final String TYPE_CONSTANTANNOTATION = "constantannotation"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_CONSTANTANNOTATION; //$NON-NLS-1$
	private final SolrAnnotation annotation;

	public SolrConstantAnnotation(SolrAnnotation annotation) {
		super(TYPE_CONSTANTANNOTATION);
		this.annotation = annotation;
	}

	public SolrAnnotation getValue() {
		return this.annotation;
	}

	@Override
	public Constant getConstant() {
		return null;
	}
}
