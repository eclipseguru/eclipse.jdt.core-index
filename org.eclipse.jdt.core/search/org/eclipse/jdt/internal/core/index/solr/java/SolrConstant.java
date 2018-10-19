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

import static java.util.Objects.requireNonNull;

import org.apache.solr.common.SolrDocument;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public abstract class SolrConstant extends SolrDocumentBasedElement {

	private static final String ARRAYPARENT = "arrayparent"; //$NON-NLS-1$

	public static SolrConstant create(Constant constant) {
		if (constant == Constant.NotAConstant) {
			return null;
		}

		switch (constant.typeID()) {
			case TypeIds.T_boolean:
				return new SolrConstantBoolean(constant.booleanValue());
			case TypeIds.T_byte:
				return new SolrConstantByte(constant.byteValue());
			case TypeIds.T_char:
				return new SolrConstantChar(constant.charValue());
			case TypeIds.T_double:
				return new SolrConstantDouble(constant.doubleValue());
			case TypeIds.T_float:
				return new SolrConstantFloat(constant.floatValue());
			case TypeIds.T_int:
				return new SolrConstantInt(constant.intValue());
			case TypeIds.T_long:
				return new SolrConstantLong(constant.longValue());
			case TypeIds.T_short:
				return new SolrConstantShort(constant.shortValue());
			case TypeIds.T_JavaLangString:
				return new SolrConstantString(constant.stringValue());
			default:
				throw new IllegalArgumentException("Unknown typeID() " + constant.typeID()); //$NON-NLS-1$
		}
	}

	protected SolrConstant(SolrDocument document) {
		super(document);
	}

	protected SolrConstant(String documentType) {
		super(documentType);
	}

	/**
	 * Returns the {@link Constant} corresponding to the value of this {@link SolrConstant} or null if the receiver
	 * corresponds to a {@link Constant}.
	 */
	public abstract Constant getConstant();

	public void setParent(SolrConstantArray parent) {
		this.document.setField(ARRAYPARENT, requireNonNull(parent.getDocumentId(), "invalid parent - missing document id")); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		try {
			return getConstant().toString();
		} catch (RuntimeException e) {
			// This is called most often from the debugger, so we want to return something meaningful even
			return super.toString();
		}
	}
}
