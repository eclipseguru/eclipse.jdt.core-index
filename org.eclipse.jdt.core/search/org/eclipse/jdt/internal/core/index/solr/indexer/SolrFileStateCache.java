package org.eclipse.jdt.internal.core.index.solr.indexer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SolrFileStateCache {

	private final ConcurrentMap<String, Boolean> fileStateCache = new ConcurrentHashMap<>();

	/**
	 * Returns true if the file at the given path is in sync with the index. Returns false if the file has already been
	 * tested and might be out-of-sync. Returns null if its status is unknown and needs to be tested.
	 *
	 * @param location
	 *            an absolute path on the filesystem
	 */
	public Boolean isUpToDate(String location) {
		return this.fileStateCache.get(location);
	}

	/**
	 * Inserts a new entry into the cache.
	 * 
	 * @param location
	 *            absolute filesystem path to the file
	 * @param result
	 *            true if the file is definitely in sync with the index, false if there is any possibility of it being
	 *            out of sync.
	 */
	public void put(String location, boolean result) {
		this.fileStateCache.put(location, result);
	}

	/**
	 * Clears the entire cache.
	 */
	public void clear() { 
		
		this.fileStateCache.clear();
	}

	/**
	 * Removes a single entry from the cache.
	 * 
	 * @param location
	 *            absolute filesystem path to the file.
	 */
	public void remove(String location) {
		this.fileStateCache.remove(location);
	}
}
