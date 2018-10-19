package org.eclipse.jdt.internal.core.index.solr.embedded;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.solr.core.CoreContainer;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs/bootstraps an embedded Solr Server
 */
public class EmbeddedSolrManager {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSolrManager.class);

	static final AtomicReference<CoreContainer> coreContainerRef = new AtomicReference<CoreContainer>();
	static volatile File solrHome;

	private static void startEmbeddedSolrServer() throws Exception {
		// the configuration template
		final File configTemplate = new File(
				FileLocator.toFileURL(JavaCore.getPlugin().getBundle().getEntry("conf-embeddedsolr")).getFile()); //$NON-NLS-1$

		// get embedded Solr home directory
		final IPath instanceLocation = JavaCore.getPlugin().getStateLocation();

		solrHome = instanceLocation.append("solr").toFile();
		if (!solrHome.isDirectory()) {
			// initialize dir
			solrHome.mkdirs();
		}

		// check for config file
		final File configFile = new File(solrHome, "solr.xml");
		if (!configFile.isFile()) {
			// deploy base configuration
			FileUtils.copyDirectory(configTemplate, solrHome);
			if (!configFile.isFile()) {
				throw new IllegalStateException("config file '" + configFile.getPath() + "' is missing");
			}
		}

		// create core container
		final CoreContainer coreContainer = new CoreContainer(solrHome.getAbsolutePath());
		if (!coreContainerRef.compareAndSet(null, coreContainer)) {
			// already initialized
			return;
		}

		// load configuration
		try {
			coreContainer.load();
		} catch (Exception e) {
			coreContainer.shutdown();
			throw e;
		}
	}

	public static CoreContainer getEmbeddedSolr() throws Exception {
		// start server (but only once, i.e. if never initialized before)
		if(solrHome == null) {
			synchronized (EmbeddedSolrManager.class) {
				if (solrHome == null) {
					LOG.debug("Starting embedded Solr server.");
					startEmbeddedSolrServer();
				}
			}
		}
		
		CoreContainer coreContainer = coreContainerRef.get();
		if (coreContainer == null)
			throw new IllegalStateException("Embedded Solr inactive!");

		return coreContainer;
	}

	public static void shutdown() {
		LOG.debug("Stopping embedded Solr server.");

		final CoreContainer coreContainer = coreContainerRef.getAndSet(null);
		if (null == coreContainer) {
			return;
		}

		coreContainer.shutdown();
	}

}
