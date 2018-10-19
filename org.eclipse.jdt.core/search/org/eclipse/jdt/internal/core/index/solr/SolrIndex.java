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
package org.eclipse.jdt.internal.core.index.solr;

import static java.lang.String.format;
import static org.eclipse.jdt.internal.core.index.solr.indexer.IndexLog.log;
import static org.eclipse.jdt.internal.core.index.solr.indexer.IndexLog.logInfo;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.eclipse.jdt.internal.core.index.solr.embedded.EmbeddedSolrManager;
import org.eclipse.jdt.internal.core.index.solr.java.SolrDocumentBasedElement;
import org.eclipse.jdt.internal.core.index.solr.java.SolrResourceFile;
import org.eclipse.jdt.internal.core.index.solr.java.SolrType;

/**
 * Provides access to the Solr based index.
 */
public class SolrIndex {

	public static boolean DEBUG_SEARCH;

	private static volatile SolrIndex globalIndex;
	
	public static SolrIndex getGlobalIndex() {
		SolrIndex index = globalIndex;
		if (index == null) {
			CoreContainer coreContainer;
			try {
				coreContainer = EmbeddedSolrManager.getEmbeddedSolr();
			} catch (Exception e) {
				throw new IllegalStateException("Unable to create Solr instance!", e);
			}
			synchronized (SolrIndex.class) {
				if (globalIndex == null) {
					globalIndex = index = new SolrIndex(new EmbeddedSolrServer(coreContainer, "java"));
				} else {
					index = globalIndex;
				}
			}
		}
		return index;
	}

	private final EmbeddedSolrServer solr;

	public SolrIndex(EmbeddedSolrServer embeddedSolrServer) {
		this.solr = embeddedSolrServer;
	}

	public SolrType findType(String name, SolrResourceFile resourceFile) {
		try {
			// build query
			final SolrQuery query = new SolrQuery();
			query.setQuery("name:" + ClientUtils.escapeQueryChars(name));
			query.setStart(0).setRows(1);
			query.setFields("*");
			query.addFilterQuery(SolrType.TYPE_FILTER, "resource:"+resourceFile.getDocumentId());

			// query
			final QueryResponse response = query(query);
			final SolrDocumentList results = response.getResults();

			// check for result
			if (!results.isEmpty()) {
				return new SolrType(results.iterator().next(), resourceFile);
			}

		} catch (SolrServerException | IOException e) {
			log("findType failed: " + e.getMessage(), e);
		}

		return null;
	}

	public void flush() {
		try {
			solr.commit(true, true);
		} catch (SolrServerException | IOException e) {
			log("Flush failed", e);
		}
	}

	public SolrResourceFile getResourceFile(String location) {
		try {
			// build query
			final SolrQuery query = new SolrQuery();
			query.setQuery("id:resource_" + ClientUtils.escapeQueryChars(location));
			query.setStart(0).setRows(1);
			query.setFields("*");
			query.addFilterQuery(SolrResourceFile.TYPE_FILTER);

			// query
			final QueryResponse response = query(query);
			final SolrDocumentList results = response.getResults();

			// check for result
			if (!results.isEmpty()) {
				return new SolrResourceFile(results.iterator().next());
			}

		} catch (SolrServerException | IOException e) {
			log("getResourceFile failed: " + e.getMessage(), e);
		}

		return null;
	}

	public void publish(SolrResourceFile resourceFile) {
		try {
			SolrInputDocument doc = new SolrInputDocument();
			for (Entry<String, Object> entry : resourceFile.getDocument()) {
				doc.setField(entry.getKey(), entry.getValue());
			}
			
			// generate id if missing
			if(doc.get(SolrDocumentBasedElement.ID) == null) {
				doc.setField(SolrDocumentBasedElement.ID, UUID.randomUUID().toString());

			}
			
			this.solr.add(doc);
		} catch (SolrServerException | IOException e) {
			log("publish failed: " + e.getMessage(), e);
		}
		
	}

	private QueryResponse query(SolrQuery solrQuery) throws SolrServerException, IOException {
		if (DEBUG_SEARCH) {
			logInfo(format("[SEARCH] %s", solrQuery));
		}

		return this.solr.query(solrQuery);
	}

}
