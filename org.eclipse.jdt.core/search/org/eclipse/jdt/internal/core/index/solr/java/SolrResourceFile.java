package org.eclipse.jdt.internal.core.index.solr.java;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;

/**
 * Represents a source of java classes (such as a .jar or .class file).
 */
public class SolrResourceFile extends SolrDocumentBasedElement{
	
	public static String getId(String location) {
		return TYPE_RESOURCEFILE + "___" + ClientUtils.escapeQueryChars(location) + "___"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static final String JDK_LEVEL = "jdkLevel"; //$NON-NLS-1$
	private static final String MANIFEST_CONTENT = "manifestContent"; //$NON-NLS-1$
	private static final String ZIP_ENTRY = "zipEntry"; //$NON-NLS-1$
	private static final String PACKAGE_FRAGMENT_ROOT = "packageFragmentRoot"; //$NON-NLS-1$
	private static final String LOCATION = "location"; //$NON-NLS-1$
	private static final String TIME_LAST_USED = "timeLastUsed"; //$NON-NLS-1$

	private static final String TYPE_RESOURCEFILE = "resourcefile"; //$NON-NLS-1$
	public static final String TYPE_FILTER = "type:" + TYPE_RESOURCEFILE; //$NON-NLS-1$

	public SolrResourceFile(SolrDocument document) {
		super(document);
	}

	public SolrResourceFile() {
		super(TYPE_RESOURCEFILE);
	}

	public void setTimeLastUsed(long timeLastUsed) {
		this.document.setField(TIME_LAST_USED, new Long(timeLastUsed));
		
	}

	public void setLocation(String location) {
		this.document.setField(LOCATION, location);
	}

	public void setPackageFragmentRoot(String location) {
		this.document.setField(PACKAGE_FRAGMENT_ROOT, location);
	}

	public String getLocation() {
		return (String) this.document.get(LOCATION);
	}

	public void addZipEntry(String fileName) {
		this.document.addField(ZIP_ENTRY, fileName);
	}

	public void setManifestContent(char[] chars) {
		this.document.setField(MANIFEST_CONTENT, new String(chars));
	}

	public void setJdkLevel(long version) {
		this.document.setField(JDK_LEVEL, new Long(version));
	}
	
}
