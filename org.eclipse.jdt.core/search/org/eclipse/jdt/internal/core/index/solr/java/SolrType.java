package org.eclipse.jdt.internal.core.index.solr.java;

import static java.util.Objects.requireNonNull;

import org.apache.solr.common.SolrDocument;

public class SolrType extends SolrDocumentBasedElement {

	private static final String RESOURCE = "resource"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$

	private final SolrResourceFile resourceFile;

	private static final String TYPE_TYPE = "type"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_TYPE; //$NON-NLS-1$

	public SolrType(SolrDocument solrDocument, SolrResourceFile resourceFile) {
		super(solrDocument);
		this.resourceFile = resourceFile;
	}

	public SolrType(SolrResourceFile resource) {
		super(TYPE_TYPE);
		this.resourceFile = resource;
		this.document.setField(RESOURCE, requireNonNull(resource.getDocumentId(), "Invalid resource - must have a document id!"));
	}

	public SolrResourceFile getResourceFile() {
		return this.resourceFile;
	}

	public void setName(String name) {
		this.document.setField(NAME, name);
	}

	public void addAnnotation(SolrTypeAnnotation annotation) {
		// TODO Auto-generated method stub
		
	}

}
