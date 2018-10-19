/*******************************************************************************
 * Copyright (c) 2018 Salesforce and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Gunnar Wagenknecht (Salesforce) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.index.solr.indexer;

import static org.eclipse.jdt.internal.compiler.util.Util.UTF_8;
import static org.eclipse.jdt.internal.compiler.util.Util.getInputStreamAsCharArray;
import static org.eclipse.jdt.internal.core.index.solr.indexer.IndexLog.log;
import static org.eclipse.jdt.internal.core.index.solr.indexer.IndexLog.logInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.index.solr.SolrIndex;
import org.eclipse.jdt.internal.core.index.solr.java.SolrResourceFile;
import org.eclipse.jdt.internal.core.nd.IReader;
import org.eclipse.jdt.internal.core.nd.db.Database;
import org.eclipse.jdt.internal.core.nd.db.IndexException;
import org.eclipse.jdt.internal.core.nd.indexer.FileStateCache;
import org.eclipse.jdt.internal.core.nd.indexer.IndexTester;
import org.eclipse.jdt.internal.core.nd.indexer.IndexerEvent;
import org.eclipse.jdt.internal.core.nd.indexer.WorkspaceSnapshot;
import org.eclipse.jdt.internal.core.nd.indexer.Indexer.Listener;
import org.eclipse.jdt.internal.core.nd.java.FileFingerprint;
import org.eclipse.jdt.internal.core.nd.java.JavaIndex;
import org.eclipse.jdt.internal.core.nd.java.JavaNames;
import org.eclipse.jdt.internal.core.nd.java.NdResourceFile;
import org.eclipse.jdt.internal.core.nd.java.NdType;
import org.eclipse.jdt.internal.core.nd.java.NdTypeId;
import org.eclipse.jdt.internal.core.nd.java.NdWorkspaceLocation;
import org.eclipse.jdt.internal.core.nd.java.TypeRef;
import org.eclipse.jdt.internal.core.nd.java.FileFingerprint.FingerprintTestResult;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryTypeDescriptor;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryTypeFactory;
import org.eclipse.jdt.internal.core.nd.java.model.IndexBinaryType;

/**
 * Central tool for indexing into Solr
 */
public class SolrIndexer {

	public static boolean DEBUG;
	public static boolean DEBUG_SELFTEST;

	// This is an arbitrary constant that is larger than the maximum number of ticks
	// reported by SubMonitor and small enough that it won't overflow a long when multiplied by a large
	// database size.
	private final static int TOTAL_TICKS_TO_REPORT_DURING_INDEXING = 1000;

	private static volatile SolrIndexer instance;

	/**
	 * @return the singleton instance (for use within JDT Core)
	 */
	public static SolrIndexer getInstance() {
		SolrIndexer singleton = instance;
		if (singleton == null) {
			synchronized (SolrIndexer.class) {
				if (instance == null) {
					instance = singleton = new SolrIndexer(SolrIndex.getGlobalIndex(),
							ResourcesPlugin.getWorkspace().getRoot());
				} else {
					singleton = instance;
				}
			}
		}
		return singleton;
	}

	private final Job rescanJob = Job.create("Updating Solr based Java index", monitor -> {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		try {
			rescan(subMonitor);
		} catch (IndexException e) {
			log("Database corruption detected during indexing. Deleting and rebuilding the index.", e); //$NON-NLS-1$
			// If we detect corruption during indexing, delete and rebuild the entire index
			rebuildIndex(subMonitor);
		}
	});

	private final SolrIndex index;
	private final SolrFileStateCache fileStateCache;
	private final IWorkspaceRoot root;

	public SolrIndexer(SolrIndex index, IWorkspaceRoot root) {
		this.index = index;
		this.root = root;
		this.fileStateCache = new SolrFileStateCache();
	}

	/**
	 * Clean up unneeded files here, but only do so if it's been a long time since the file was last referenced. Being
	 * too eager about removing old files means that operations which temporarily cause a file to become unreferenced
	 * will run really slowly. also eagerly clean up any partially-indexed files we discover during the scan. That is,
	 * if we discover a file with a timestamp of 0, it indicates that the indexer or all of Eclipse crashed midway
	 * through indexing the file. Such garbage should be cleaned up as soon as possible, since it will never be useful.
	 *
	 * @param currentTimeMillis
	 *            timestamp of the time at which the indexing operation started
	 * @param allIndexables
	 *            list of all referenced java roots
	 * @param monitor
	 *            progress monitor
	 * @return the number of indexables in the index, prior to garbage collection
	 */
	private int cleanGarbage(long currentTimeMillis, Collection<IPath> allIndexables, IProgressMonitor monitor) {
		// FIXME: In Solr we may be able to delete by query, i.a. any doc (file) that's really old
		return 0;
	}

	/**
	 * Given a list of fragment roots, returns the subset of roots that have changed since the last time they were
	 * indexed.
	 */
	private List<IPath> getIndexablesThatHaveChanged(Collection<IPath> indexables,
			Map<IPath, FingerprintTestResult> fingerprints) {
		List<IPath> indexablesWithChanges = new ArrayList<>();
		for (IPath next : indexables) {
			FingerprintTestResult testResult = fingerprints.get(next);

			if (!testResult.matches()) {
				indexablesWithChanges.add(next);
			}
		}
		return indexablesWithChanges;
	}

	/**
	 * Flags a specific file system path (file or folder) for re-indexing.
	 * <p>
	 * If the path maps to a folder, all its children will be re-indexed as well.
	 * </p>
	 * 
	 * @param path
	 *            the path to re-index
	 */
	public void makeDirty(IPath location) {
		this.fileStateCache.remove(location.toString());
		rescanAll();
	}

	/**
	 * Flags a full project for re-indexing
	 * 
	 * @param project
	 *            the project to re-index
	 */
	public void makeDirty(IProject project) {
		// TODO Auto-generated method stub

	}

	/**
	 * Flags a specific workspace path (file or folder) for re-indexing.
	 * <p>
	 * If the path maps to a folder, all its children will be re-indexed as well.
	 * </p>
	 * 
	 * @param path
	 *            the path to re-index
	 */
	public void makeWorkspacePathDirty(IPath path) {
		// TODO Auto-generated method stub

	}

	public void rebuildIndex(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		this.rescanJob.cancel();
		try {
			this.rescanJob.join(0, subMonitor.split(1));
		} catch (InterruptedException e) {
			// Nothing to do.
		}

		// FIXME: delete and flush solr

		rescan(subMonitor.split(97));
	}

	public void rescan(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		long currentTimeMs = System.currentTimeMillis();
		if (DEBUG) {
			logInfo("Solr Indexer running rescan"); //$NON-NLS-1$
		}

		this.fileStateCache.clear();
		WorkspaceSnapshot snapshot = WorkspaceSnapshot.create(this.root, subMonitor.split(1));
		Set<IPath> locations = snapshot.allLocations();

		long startGarbageCollectionMs = System.currentTimeMillis();

		// Remove all files in the index which aren't referenced in the workspace
		int gcFiles = cleanGarbage(currentTimeMs, locations, subMonitor.split(1));

		long startFingerprintTestMs = System.currentTimeMillis();

		Map<IPath, FingerprintTestResult> fingerprints = testFingerprints(locations, subMonitor.split(1));
		Set<IPath> indexablesWithChanges = new HashSet<>(getIndexablesThatHaveChanged(locations, fingerprints));

		// Compute the total number of bytes to be read in and indexed
		long startIndexingMs = System.currentTimeMillis();
		long totalSizeToIndex = 0;
		for (IPath next : indexablesWithChanges) {
			FingerprintTestResult nextFingerprint = fingerprints.get(next);
			totalSizeToIndex += nextFingerprint.getNewFingerprint().getSize();
		}
		double tickCoefficient = totalSizeToIndex == 0 ? 0.0
				: (double) TOTAL_TICKS_TO_REPORT_DURING_INDEXING / (double) totalSizeToIndex;

		int classesIndexed = 0;
		SubMonitor loopMonitor = subMonitor.split(94).setWorkRemaining(TOTAL_TICKS_TO_REPORT_DURING_INDEXING);
		for (IPath next : indexablesWithChanges) {
			FingerprintTestResult nextFingerprint = fingerprints.get(next);
			int ticks = (int) (nextFingerprint.getNewFingerprint().getSize() * tickCoefficient);

			classesIndexed += rescanArchive(currentTimeMs, next, snapshot.get(next),
					fingerprints.get(next).getNewFingerprint(), loopMonitor.split(ticks));
		}

		long endIndexingMs = System.currentTimeMillis();

		Map<IPath, List<IJavaElement>> pathsToUpdate = new HashMap<>();

		for (IPath next : locations) {
			if (!indexablesWithChanges.contains(next)) {
				pathsToUpdate.put(next, snapshot.get(next));
				continue;
			}
		}

		updateResourceMappings(pathsToUpdate, subMonitor.split(1));

		// Flush the index to disk
		this.index.flush();

		fireDelta(indexablesWithChanges, subMonitor.split(1));

		if (DEBUG) {
			logInfo("Rescan finished"); //$NON-NLS-1$
		}

		long endResourceMappingMs = System.currentTimeMillis();

		long locateIndexablesTimeMs = startGarbageCollectionMs - currentTimeMs;
		long garbageCollectionMs = startFingerprintTestMs - startGarbageCollectionMs;
		long fingerprintTimeMs = startIndexingMs - startFingerprintTestMs;
		long indexingTimeMs = endIndexingMs - startIndexingMs;
		long resourceMappingTimeMs = endResourceMappingMs - endIndexingMs;

		double averageGcTimeMs = gcFiles == 0 ? 0 : (double) garbageCollectionMs / (double) gcFiles;
		double averageIndexTimeMs = classesIndexed == 0 ? 0 : (double) indexingTimeMs / (double) classesIndexed;
		double averageFingerprintTimeMs = locations.size() == 0 ? 0
				: (double) fingerprintTimeMs / (double) locations.size();
		double averageResourceMappingMs = pathsToUpdate.size() == 0 ? 0
				: (double) resourceMappingTimeMs / (double) pathsToUpdate.size();

		if (DEBUG) {
			DecimalFormat msFormat = new DecimalFormat("#0.###"); //$NON-NLS-1$
			DecimalFormat percentFormat = new DecimalFormat("#0.###"); //$NON-NLS-1$
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS\n"); //$NON-NLS-1$
			System.out.println("Indexing done at " + format.format(new Date(endResourceMappingMs)) //$NON-NLS-1$
					+ "  Located " + locations.size() + " indexables in " + locateIndexablesTimeMs + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (gcFiles != 0) {
				System.out.println("  Collected garbage from " + gcFiles + " files in " + garbageCollectionMs //$NON-NLS-1$//$NON-NLS-2$
						+ "ms, average time = " + msFormat.format(averageGcTimeMs) + "ms"); //$NON-NLS-1$//$NON-NLS-2$
			}
			System.out.println("  Tested " + locations.size() + " fingerprints in " + fingerprintTimeMs //$NON-NLS-1$ //$NON-NLS-2$
					+ "ms, average time = " + msFormat.format(averageFingerprintTimeMs) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			if (classesIndexed != 0) {
				System.out.println("  Indexed " + classesIndexed + " classes (from " + indexablesWithChanges.size() //$NON-NLS-1$//$NON-NLS-2$
						+ " files containing " + Database.formatByteString(totalSizeToIndex) + ") in " + indexingTimeMs //$NON-NLS-1$ //$NON-NLS-2$
						+ "ms, average time per class = " + msFormat.format(averageIndexTimeMs) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (pathsToUpdate.size() != 0) {
				System.out.println("  Updated " + pathsToUpdate.size() + " paths in " + resourceMappingTimeMs //$NON-NLS-1$//$NON-NLS-2$
						+ "ms, average time = " + msFormat.format(averageResourceMappingMs) + "ms"); //$NON-NLS-1$//$NON-NLS-2$
			}

			double totalTimeMs = endResourceMappingMs - currentTimeMs;
			System.out.println("  Total indexing time = " + msFormat.format(totalTimeMs) + "ms"); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private void updateResourceMappings(Map<IPath, List<IJavaElement>> pathsToUpdate, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, pathsToUpdate.keySet().size());

		for (Entry<IPath, List<IJavaElement>> entry : pathsToUpdate.entrySet()) {
			SubMonitor iterationMonitor = subMonitor.split(1).setWorkRemaining(10);

			SolrResourceFile resourceFile = this.index.getResourceFile(entry.getKey().toString());
			if (resourceFile == null) {
				continue;
			}

			attachWorkspaceFilesToResource(entry.getValue(), resourceFile);
		}
	}

	/**
	 * Kicks off a full indexing in the background.
	 */
	public void rescanAll() {
		this.fileStateCache.clear();
		rescanAll();
	}

	private Map<IPath, FingerprintTestResult> testFingerprints(Collection<IPath> allIndexables,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, allIndexables.size());
		Map<IPath, FingerprintTestResult> result = new HashMap<>();

		for (IPath next : allIndexables) {
			result.put(next, testForChanges(next, subMonitor.split(1)));
		}

		return result;
	}

	private FingerprintTestResult testForChanges(IPath thePath, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		String pathString = thePath.toString();

		subMonitor.split(50);
		NdResourceFile resourceFile = null;
		FileFingerprint fingerprint = FileFingerprint.getEmpty();
		NdResourceFile resourceFile = this.index.getResourceFile(pathString.toCharArray());

		if (resourceFile != null) {
			fingerprint = resourceFile.getFingerprint();
		}

		FingerprintTestResult result = fingerprint.test(thePath, subMonitor.split(40));

		// If this file hasn't changed but its timestamp has, write an updated fingerprint to the database
		if (resourceFile != null && result.matches() && result.needsNewFingerprint()) {
			if (resourceFile.isInIndex()) {
				if (DEBUG) {
					logInfo("Writing updated fingerprint for " + thePath + ": " + result.getNewFingerprint()); //$NON-NLS-1$//$NON-NLS-2$
				}
				resourceFile.setFingerprint(result.getNewFingerprint());
			}
		}

		return result;
	}

	/**
	 * Rescans an archive (a jar, zip, or class file on the filesystem). Returns the number of classes indexed.
	 * 
	 * @throws JavaModelException
	 */
	private int rescanArchive(long currentTimeMillis, IPath thePath, List<IJavaElement> elementsMappingOntoLocation,
			FileFingerprint fingerprint, IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		if (elementsMappingOntoLocation.isEmpty()) {
			return 0;
		}

		IJavaElement element = elementsMappingOntoLocation.get(0);

		String pathString = thePath.toString();

		SolrResourceFile resourceFile = new SolrResourceFile();
		resourceFile.setTimeLastUsed(currentTimeMillis);
		resourceFile.setLocation(pathString);
		IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) element
				.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		IPath rootPathString = JavaIndex.getLocationForElement(packageFragmentRoot);
		if (!rootPathString.equals(thePath)) {
			resourceFile.setPackageFragmentRoot(rootPathString.toString());
		}
		attachWorkspaceFilesToResource(elementsMappingOntoLocation, resourceFile);
		this.index.publish(resourceFile);

		if (DEBUG) {
			logInfo("rescanning " + thePath.toString() + ", " + fingerprint); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int result = 0;
		try {
			if (fingerprint.fileExists()) {
				result = addElement(resourceFile, element, subMonitor.split(50));
			}
		} catch (JavaModelException e) {
			if (DEBUG) {
				log("the file " + pathString + " cannot be indexed due to a recoverable error", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// If this file can't be indexed due to a recoverable error, delete the NdResourceFile entry for it.
			if (resourceFile.isInIndex()) {
				resourceFile.delete();
			}
			return 0;
		} catch (RuntimeException e) {
			if (DEBUG) {
				log("A RuntimeException occurred while indexing " + pathString, e); //$NON-NLS-1$
			}
			throw e;
		} catch (FileNotFoundException e) {
			fingerprint = FileFingerprint.getEmpty();
		}

		if (DEBUG && !fingerprint.fileExists()) {
			log("the file " + pathString + " was not indexed because it does not exist", null); //$NON-NLS-1$ //$NON-NLS-2$
		}

		List<NdResourceFile> allResourcesWithThisPath = Collections.emptyList();
		// Now update the timestamp and delete all older versions of this resource that exist in the index
		if (resourceFile.isInIndex()) {
			resourceFile.setFingerprint(fingerprint);
			allResourcesWithThisPath = this.index.findResourcesWithPath(pathString);
			// Remove this file from the file state cache, since the act of indexing it may have changed its
			// up-to-date status. Note that it isn't necessarily up-to-date now -- it may have changed again
			// while we were indexing it.
			this.fileStateCache.remove(resourceFile.getLocation().getString());
		}

		SubMonitor deletionMonitor = subMonitor.split(40).setWorkRemaining(allResourcesWithThisPath.size() - 1);
		for (NdResourceFile next : allResourcesWithThisPath) {
			if (!next.equals(resourceFile)) {
				deleteResource(next, deletionMonitor.split(1));
			}
		}

		return result;
	}

	private void attachWorkspaceFilesToResource(List<IJavaElement> elementsMappingOntoLocation,
			SolrResourceFile resourceFile) {
		for (IJavaElement next : elementsMappingOntoLocation) {
			IResource nextResource = next.getResource();
			if (nextResource != null) {
				// FIXME: new NdWorkspaceLocation(this.nd, resourceFile,
				// nextResource.getFullPath().toString().toCharArray());
			}
		}
	}

	/**
	 * Adds an archive to the index, under the given NdResourceFile.
	 * 
	 * @throws FileNotFoundException
	 *             if the file does not exist
	 */
	private int addElement(SolrResourceFile resourceFile, IJavaElement element, IProgressMonitor monitor)
			throws JavaModelException, FileNotFoundException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		if (element instanceof JarPackageFragmentRoot) {
			JarPackageFragmentRoot jarRoot = (JarPackageFragmentRoot) element;

			IPath workspacePath = jarRoot.getPath();
			IPath location = JavaIndex.getLocationForElement(jarRoot);

			int classesIndexed = 0;
			try (ZipFile zipFile = new ZipFile(JavaModelManager.getLocalFile(jarRoot.getPath()))) {
				// Used for the error-handling unit tests
				if (JavaModelManager.throwIoExceptionsInGetZipFile) {
					if (DEBUG) {
						logInfo("Throwing simulated IOException for error handling test case"); //$NON-NLS-1$
					}
					throw new IOException();
				}
				subMonitor.setWorkRemaining(zipFile.size());

				for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
					SubMonitor nextEntry = subMonitor.split(1).setWorkRemaining(2);
					ZipEntry member = e.nextElement();
					String fileName = member.getName();
					boolean classFileName = org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(fileName);
					if (member.isDirectory() || !classFileName) {
						if (DEBUG) {
							logInfo("Inserting non-class file " + fileName + " into " //$NON-NLS-1$//$NON-NLS-2$
									+ resourceFile.getLocation()); // $NON-NLS-1$
						}
						resourceFile.addZipEntry(fileName);

						if (fileName.equals(TypeConstants.META_INF_MANIFEST_MF)) {
							try (InputStream inputStream = zipFile.getInputStream(member)) {
								char[] chars = getInputStreamAsCharArray(inputStream, -1, UTF_8);

								resourceFile.setManifestContent(chars);
							}
						}
					}
					if (member.isDirectory()) {
						// Note that non-empty directories are stored implicitly (as the parent directory of a file
						// or class within the jar). Empty directories are not currently stored in the index.
						continue;
					}
					nextEntry.split(1);

					if (classFileName) {
						String binaryName = fileName.substring(0,
								fileName.length() - SuffixConstants.SUFFIX_STRING_class.length());
						char[] fieldDescriptor = JavaNames.binaryNameToFieldDescriptor(binaryName.toCharArray());
						String indexPath = jarRoot.getHandleIdentifier() + IDependent.JAR_FILE_ENTRY_SEPARATOR
								+ binaryName;
						BinaryTypeDescriptor descriptor = new BinaryTypeDescriptor(location.toString().toCharArray(),
								fieldDescriptor, workspacePath.toString().toCharArray(), indexPath.toCharArray());
						try {
							byte[] contents = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(member,
									zipFile);
							ClassFileReader classFileReader = new ClassFileReader(contents, descriptor.indexPath, true);
							if (addClassToIndex(resourceFile, descriptor.fieldDescriptor, descriptor.indexPath,
									classFileReader, nextEntry.split(1))) {
								classesIndexed++;
							}
						} catch (CoreException | ClassFormatException exception) {
							log("Unable to index " + descriptor.toString(), exception); //$NON-NLS-1$
						}
					}
				}
			} catch (ZipException e) {
				log("The zip file " + jarRoot.getPath() + " was corrupt", e); //$NON-NLS-1$//$NON-NLS-2$
				// Indicates a corrupt zip file. Treat this like an empty zip file.
				if (resourceFile.isInIndex()) {
					resourceFile.setFlags(NdResourceFile.FLG_CORRUPT_ZIP_FILE);
				}
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException ioException) {
				throw new JavaModelException(ioException, IJavaModelStatusConstants.IO_EXCEPTION);
			} catch (CoreException coreException) {
				throw new JavaModelException(coreException);
			}

			if (DEBUG && classesIndexed == 0) {
				logInfo("The path " + element.getPath() + " contained no class files"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return classesIndexed;
		} else if (element instanceof IOrdinaryClassFile) {
			IOrdinaryClassFile classFile = (IOrdinaryClassFile) element;

			SubMonitor iterationMonitor = subMonitor.split(1);
			BinaryTypeDescriptor descriptor = BinaryTypeFactory.createDescriptor(classFile);

			try {
				ClassFileReader classFileReader = BinaryTypeFactory.rawReadTypeTestForExists(descriptor, true, false);
				if (classFileReader != null) {
					addClassToIndex(resourceFile, descriptor.fieldDescriptor, descriptor.indexPath, classFileReader,
							iterationMonitor);
				}
			} catch (CoreException | ClassFormatException e) {
				log("Unable to index " + classFile.toString(), e); //$NON-NLS-1$
				return 0;
			}

			return 1;
		} else {
			logInfo("Unable to index elements of type " + element); //$NON-NLS-1$
			return 0;
		}
	}

	private void addClassToIndex(SolrResourceFile resourceFile, char[] fieldDescriptor, char[] indexPath,
			ClassFileReader binaryType, IProgressMonitor monitor) throws ClassFormatException, CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		ClassFileToIndexConverter converter = new ClassFileToIndexConverter(resourceFile, this.index);

		if (DEBUG) {
			logInfo("Inserting " + new String(fieldDescriptor) + " into " + resourceFile.getLocation()); //$NON-NLS-1$//$NON-NLS-2$
		}
		converter.addType(binaryType, fieldDescriptor, subMonitor.split(45));
		resourceFile.setJdkLevel(binaryType.getVersion());

		if (DEBUG_SELFTEST) {
			// When this debug flag is on, we test everything written to the index by reading it back immediately after
			// indexing and comparing it with the original class file.
			try {
				NdType targetType = this.index.findType(new String(fieldDescriptor), resourceFile);
				if (targetType != null) {
					IndexBinaryType actualType = new IndexBinaryType(TypeRef.create(targetType), indexPath);
					IndexTester.testType(binaryType, actualType);
				} else {
					logInfo("Could not find class in index immediately after indexing it: " + new String(indexPath)); //$NON-NLS-1$
				}
			} catch (RuntimeException e) {
				log("Error during indexing: " + new String(indexPath), e); //$NON-NLS-1$
			}
		}
	}

	private void fireDelta(Set<IPath> indexablesWithChanges, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		IProject[] projects = this.root.getProjects();

		List<IProject> projectsToScan = new ArrayList<>();

		for (IProject next : projects) {
			if (next.isOpen()) {
				projectsToScan.add(next);
			}
		}
		JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
		boolean hasChanges = false;
		JavaElementDelta delta = new JavaElementDelta(model);
		SubMonitor projectLoopMonitor = subMonitor.split(1).setWorkRemaining(projectsToScan.size());
		for (IProject project : projectsToScan) {
			projectLoopMonitor.split(1);
			try {
				if (project.isOpen() && project.isNatureEnabled(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);

					IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();

					for (IPackageFragmentRoot next : roots) {
						if (next.isArchive()) {
							IPath location = JavaIndex.getLocationForElement(next);

							if (indexablesWithChanges.contains(location)) {
								hasChanges = true;
								delta.changed(next,
										IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED);
							}
						}
					}
				}
			} catch (CoreException e) {
				log(e);
			}
		}

		if (hasChanges) {
			fireChange(IndexerEvent.createChange(delta));
		}
	}

	private void fireChange(IndexerEvent event) {
		// FIXME: implement
	}

}
