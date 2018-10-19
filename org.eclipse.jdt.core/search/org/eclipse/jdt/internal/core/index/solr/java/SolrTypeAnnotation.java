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
public class SolrTypeAnnotation extends SolrAnnotation {

	private final SolrType annotatedType;

	private static final String TYPE_TYPEANNOTATION = "typeannotation"; //$NON-NLS-1$
	@SuppressWarnings("hiding")
	public static final String TYPE_FILTER = "type:" + TYPE_TYPEANNOTATION; //$NON-NLS-1$
	
	public SolrTypeAnnotation(SolrType annotatedType) {
		super(TYPE_TYPEANNOTATION);
		this.annotatedType = annotatedType;
	}

	public SolrType getAnnotatedType() {
		return this.annotatedType;
	}

	public void setTypePath(int[] typePath) {
		// TODO Auto-generated method stub
		
	}

	public void setTargetType(int targetType) {
		// TODO Auto-generated method stub
		
	}

	public void setTypeParameterIndex(int typeParameterIndex) {
		// TODO Auto-generated method stub
		
	}

	public void setSupertypeInde(int supertypeIndex) {
		// TODO Auto-generated method stub
		
	}

	public void setBoundIndex(int boundIndex) {
		// TODO Auto-generated method stub
		
	}

	public void setMethodFormalParameterIndex(int methodFormalParameterIndex) {
		// TODO Auto-generated method stub
		
	}

	public void setThrowsTypeIndex(int throwsTypeIndex) {
		// TODO Auto-generated method stub
		
	}

}
