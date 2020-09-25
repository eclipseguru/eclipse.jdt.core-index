/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.index.Index;

public class SubTypeSearchJob extends PatternSearchJob {

HashSet<Index> indexes = new HashSet<>(5);

public SubTypeSearchJob(SearchPattern pattern, SearchParticipant participant, IJavaSearchScope scope, IndexQueryRequestor requestor) {
	super(pattern, participant, scope, requestor);
}
public void finished() {
	this.indexes.parallelStream().forEach(Index::stopQuery);
}
@Override
public Index[] getIndexes(IProgressMonitor progressMonitor) {
	if (this.indexes.size() == 0) {
		return super.getIndexes(progressMonitor);
	}
	this.areIndexesReady = true; // use stored indexes until the job's end
	return this.indexes.toArray(new Index[this.indexes.size()]);
}
@Override
public boolean search(Index index, IProgressMonitor progressMonitor) {
	if (index == null) return COMPLETE;
	if (this.indexes.add(index))
		index.startQuery();
	return super.search(index, progressMonitor);
}
}
