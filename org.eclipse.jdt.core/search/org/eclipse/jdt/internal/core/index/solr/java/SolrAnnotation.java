/**
 * Copyright (c) 2018 <company> and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     <author> - initial API and implementation
 */
package org.eclipse.jdt.internal.core.index.solr.java;

/**
 * 
 */
public class SolrAnnotation extends SolrDocumentBasedElement {

	private static final String TYPE_ANNOTATION = "annotation"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_ANNOTATION; //$NON-NLS-1$
	
	public SolrAnnotation() {
		super(TYPE_ANNOTATION);
	}
	
	protected SolrAnnotation(String type) {
		super(type);
	}

	public void setTypeName(String typeName) {
		// TODO Auto-generated method stub
		
	}
	
}
