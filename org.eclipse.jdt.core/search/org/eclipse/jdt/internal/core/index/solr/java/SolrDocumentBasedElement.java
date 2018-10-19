package org.eclipse.jdt.internal.core.index.solr.java;

import org.apache.solr.common.SolrDocument;

public abstract class SolrDocumentBasedElement {

	public static final String TYPE = "type"; //$NON-NLS-1$
	public static final String ID = "id"; //$NON-NLS-1$
	
	protected final SolrDocument document;
	
	public SolrDocumentBasedElement(SolrDocument document) {
		this.document = document;
	}

	public SolrDocumentBasedElement(String documentType) {
		this.document = new SolrDocument();
		this.document.setField(TYPE, documentType);
	}

	public SolrDocument getDocument() {
		return this.document;
	}
	
	public String getDocumentId() {
		return (String) this.document.get(ID);
	}
	
	public String getDocumentType() {
		return (String) this.document.get(TYPE);
	}
}
