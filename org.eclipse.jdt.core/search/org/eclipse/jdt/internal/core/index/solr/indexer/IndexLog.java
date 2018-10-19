package org.eclipse.jdt.internal.core.index.solr.indexer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;

public class IndexLog {
	public static String PLUGIN_ID = JavaCore.PLUGIN_ID;

	public static void log(Throwable e) {
		String msg = e.getMessage();
		if (msg == null) {
			log("Error", e); //$NON-NLS-1$
		} else {
			log("Error: " + msg, e); //$NON-NLS-1$
		}
	}

	public static void log(String message, Throwable e) {
		log(createStatus(message, e));
	}

	public static IStatus createStatus(String msg, Throwable e) {
		return new Status(IStatus.ERROR, PLUGIN_ID, msg, e);
	}

	public static IStatus createStatus(String msg) {
		return new Status(IStatus.ERROR, PLUGIN_ID, msg);
	}

	public static void logInfo(String message) {
		log(new Status(IStatus.INFO, PLUGIN_ID, message));
	}

	public static void log(IStatus status) {
		JavaCore.getPlugin().getLog().log(status);
	}
}
