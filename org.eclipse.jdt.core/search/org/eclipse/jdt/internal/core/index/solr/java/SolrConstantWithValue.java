package org.eclipse.jdt.internal.core.index.solr.java;

import org.apache.solr.common.SolrDocument;

public abstract class SolrConstantWithValue<T> extends SolrConstant {

	private static final String VALUE = "value"; //$NON-NLS-1$
	private final T defaultValue;
	
	public SolrConstantWithValue(SolrDocument document, T defaultValue) {
		super(document);
		this.defaultValue = defaultValue;
	}

	public SolrConstantWithValue(String documentType, T defaultValue) {
		super(documentType);
		this.defaultValue = defaultValue;
	}

	@SuppressWarnings("unchecked")
	public T getValue() {
		T value = (T) this.document.getFieldValue(VALUE);
		return value != null ? value : this.defaultValue; 
	}

	public void setValue(T value) {
		this.document.setField(VALUE, value);
	}
	
}
