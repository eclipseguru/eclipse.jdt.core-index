package org.eclipse.jdt.core.tests.pdom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.tests.pdom.util.BaseTestCase;
import org.eclipse.jdt.internal.core.pdom.PDOM;
import org.eclipse.jdt.internal.core.pdom.PDOMNode;
import org.eclipse.jdt.internal.core.pdom.PDOMNodeTypeRegistry;
import org.eclipse.jdt.internal.core.pdom.RawGrowableArray;
import org.eclipse.jdt.internal.core.pdom.field.FieldOneToMany;
import org.eclipse.jdt.internal.core.pdom.field.FieldInt;
import org.eclipse.jdt.internal.core.pdom.field.FieldManyToOne;
import org.eclipse.jdt.internal.core.pdom.field.StructDef;

import junit.framework.Test;

public class FieldBackPointerTest extends BaseTestCase {
	public static class ForwardPointerStruct extends PDOMNode {
		public static final FieldManyToOne<BackPointerStruct> FORWARD;
		public static final FieldManyToOne<BackPointerStruct> OWNER;

		@SuppressWarnings("hiding")
		public static final StructDef<ForwardPointerStruct> type;

		static {
			type = StructDef.create(ForwardPointerStruct.class, PDOMNode.type);

			FORWARD = FieldManyToOne.create(type, BackPointerStruct.BACK);
			OWNER = FieldManyToOne.createOwner(type, BackPointerStruct.OWNED);
			type.done();
		}

		public ForwardPointerStruct(PDOM pdom) {
			super(pdom);
		}

		public ForwardPointerStruct(PDOM pdom, long record) {
			super(pdom, record);
		}

		public void setBp(BackPointerStruct toSet) {
			FORWARD.put(getPDOM(), this.address, toSet);
		}

		public BackPointerStruct getBp() {
			return FORWARD.get(getPDOM(), this.address);
		}

		public void setOwner(BackPointerStruct owner) {
			OWNER.put(getPDOM(), this.address, owner);
		}

		public BackPointerStruct getOwner() {
			return OWNER.get(getPDOM(), this.address);
		}
	}

	public static class BackPointerStruct extends PDOMNode {
		public static final FieldOneToMany<ForwardPointerStruct> BACK;
		public static final FieldOneToMany<ForwardPointerStruct> OWNED;
		public static final FieldInt SOMEINT;
		
		@SuppressWarnings("hiding")
		public static final StructDef<BackPointerStruct> type;

		static {
			type = StructDef.create(BackPointerStruct.class, PDOMNode.type);

			BACK = FieldOneToMany.create(type, ForwardPointerStruct.FORWARD, 2);
			OWNED = FieldOneToMany.create(type, ForwardPointerStruct.OWNER, 0);
			SOMEINT = type.addInt();
			type.done();
		}

		public BackPointerStruct(PDOM pdom) {
			super(pdom);

			// Fill with nonzero values to ensure that "OWNED" doesn't read beyond its boundary
			SOMEINT.put(pdom, this.address, 0xf0f0f0f0);
		}

		public BackPointerStruct(PDOM pdom, long record) {
			super(pdom, record);
		}

		public void ensureBackPointerCapacity(int capacity) {
			BACK.ensureCapacity(getPDOM(), this.address, capacity);
		}

		public int getBackPointerCapacity() {
			return BACK.getCapacity(getPDOM(), this.address);
		}

		public List<ForwardPointerStruct> getBackPointers() {
			return BACK.asList(getPDOM(), this.address);
		}

		public List<ForwardPointerStruct> getOwned() {
			return OWNED.asList(getPDOM(), this.address);
		}

		public int backPointerSize() {
			return BACK.size(getPDOM(), this.address);
		}

		public boolean backPointersAreEmpty() {
			return BACK.isEmpty(getPDOM(), this.address);
		}

		public boolean ownedPointersAreEmpty() {
			return OWNED.isEmpty(getPDOM(), this.address);
		}

		public ForwardPointerStruct getBackPointer(int i) {
			return BACK.get(getPDOM(), this.address, i);
		}
	}

	ForwardPointerStruct fa;
	ForwardPointerStruct fb;
	ForwardPointerStruct fc;
	ForwardPointerStruct fd;
	BackPointerStruct ba;
	BackPointerStruct bb;
	private PDOM pdom;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		PDOMNodeTypeRegistry<PDOMNode> registry = new PDOMNodeTypeRegistry<>();
		registry.register(0, BackPointerStruct.type.getFactory());
		registry.register(1, ForwardPointerStruct.type.getFactory());
		this.pdom = DatabaseTestUtil.createEmptyPdom(getName(), registry);
		this.pdom.getDB().setExclusiveLock();
		this.ba = new BackPointerStruct(this.pdom);
		this.bb = new BackPointerStruct(this.pdom);
		this.fa = new ForwardPointerStruct(this.pdom);
		this.fb = new ForwardPointerStruct(this.pdom);
		this.fc = new ForwardPointerStruct(this.pdom);
		this.fd = new ForwardPointerStruct(this.pdom);
	}

	public static Test suite() {
		return BaseTestCase.suite(FieldBackPointerTest.class);
	}

	void assertBackPointers(BackPointerStruct bp, ForwardPointerStruct... fp) {
		HashSet<ForwardPointerStruct> backPointers = new HashSet<>(bp.getBackPointers());
		HashSet<ForwardPointerStruct> desired = new HashSet<>();

		desired.addAll(Arrays.asList(fp));
		assertEquals(desired, backPointers);
	}

	public void testWriteFollowedByReadReturnsSameThing() throws Exception {
		this.fa.setBp(this.ba);
		BackPointerStruct backpointer = this.fa.getBp();

		assertEquals(this.ba, backpointer);
	}

	public void testListWithoutInlineElementsCanBeEmpty() throws Exception {
		assertTrue(this.ba.ownedPointersAreEmpty());
	}

	public void testReadNull() throws Exception {
		assertEquals(null, this.fa.getBp());
	}
	
	public void testAssigningTheSamePointerTwiceIsANoop() throws Exception {
		this.fa.setBp(this.ba);

		assertBackPointers(this.ba, this.fa);

		// Now do the same thing again
		this.fa.setBp(this.ba);

		assertBackPointers(this.ba, this.fa);
	}
	
	public void testAssigningForwardPointerInsertsBackPointer() throws Exception {
		this.fa.setBp(this.ba);

		assertEquals(Arrays.asList(this.fa), this.ba.getBackPointers());
		assertEquals(1, this.ba.backPointerSize());
	}

	public void testRemovesInlineElement() throws Exception {
		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		this.fd.setBp(this.ba);

		assertEquals(4, this.ba.backPointerSize());
		this.fb.setBp(null);
		assertEquals(3, this.ba.backPointerSize());

		assertBackPointers(this.ba, this.fa, this.fc, this.fd);
	}

	public void testRemovesElementFromGrowableBlock() throws Exception {
		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		this.fd.setBp(this.ba);

		assertEquals(4, this.ba.backPointerSize());
		this.fc.setBp(null);
		assertEquals(3, this.ba.backPointerSize());

		assertBackPointers(this.ba, this.fa, this.fb, this.fd);
	}

	public void testDestructingForwardPointerRemovesBackPointer() throws Exception {
		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);

		this.fb.delete();
		this.pdom.processDeletions();

		assertBackPointers(this.ba, this.fa, this.fc);
	}

	public void testDestructingBackPointerClearsForwardPointers() throws Exception {
		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);

		this.ba.delete();
		this.pdom.processDeletions();

		assertEquals(null, this.fa.getBp());
		assertEquals(null, this.fb.getBp());
		assertEquals(null, this.fc.getBp());
	}

	public void testElementsRemainInInsertionOrderIfNoRemovals() throws Exception {
		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		this.fd.setBp(this.ba);

		assertEquals(Arrays.asList(this.fa, this.fb, this.fc, this.fd), this.ba.getBackPointers());
	}

	public void testDeletingOwnerDeletesOwned() throws Exception {
		this.fa.setBp(this.ba);
		this.fa.setOwner(this.bb);

		this.fb.setBp(this.ba);
		this.fb.setOwner(this.bb);

		this.fc.setBp(this.ba);

		this.bb.delete();
		this.pdom.processDeletions();

		assertBackPointers(this.ba, this.fc);
	}

	public void testEnsureCapacityDoesNothingIfLessThanInlineElements() throws Exception {
		this.ba.ensureBackPointerCapacity(1);
		assertEquals(2, this.ba.getBackPointerCapacity());
	}

	public void testEnsureCapacityAllocatesPowersOfTwoPlusInlineSize() throws Exception {
		this.ba.ensureBackPointerCapacity(60);
		assertEquals(66, this.ba.getBackPointerCapacity());
	}

	public void testEnsureCapacityAllocatesMinimumSize() throws Exception {
		this.ba.ensureBackPointerCapacity(3);
		assertEquals(4, this.ba.getBackPointerCapacity());
	}

	public void testEnsureCapacityClampsToChunkSize() throws Exception {
		this.ba.ensureBackPointerCapacity(RawGrowableArray.getMaxGrowableBlockSize() - 40);
		assertEquals(RawGrowableArray.getMaxGrowableBlockSize() + 2, this.ba.getBackPointerCapacity());
	}

	public void testEnsureCapacityGrowsByMultiplesOfMaxBlockSizeOnceMetablockInUse() throws Exception {
		int maxBlockSize = RawGrowableArray.getMaxGrowableBlockSize();
		this.ba.ensureBackPointerCapacity(maxBlockSize * 3 - 100);
		assertEquals(maxBlockSize * 3 + 2, this.ba.getBackPointerCapacity());
	}

	public void testAdditionsWontReduceCapacity() throws Exception {
		int maxBlockSize = RawGrowableArray.getMaxGrowableBlockSize();
		this.ba.ensureBackPointerCapacity(maxBlockSize);

		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		this.fd.setBp(this.ba);

		assertEquals(maxBlockSize + 2, this.ba.getBackPointerCapacity());
	}

	public void testIsEmpty() throws Exception {
		assertTrue(this.ba.backPointersAreEmpty());
		this.fa.setBp(this.ba);
		assertFalse(this.ba.backPointersAreEmpty());
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		this.fd.setBp(this.ba);
		assertFalse(this.ba.backPointersAreEmpty());
	}

	public void testRemovalsReduceCapacity() throws Exception {
		int maxBlockSize = RawGrowableArray.getMaxGrowableBlockSize();
		this.ba.ensureBackPointerCapacity(maxBlockSize);

		this.fa.setBp(this.ba);
		this.fb.setBp(this.ba);
		this.fc.setBp(this.ba);
		assertEquals(maxBlockSize + 2, this.ba.getBackPointerCapacity());
		
		this.fb.setBp(null);
		this.fc.setBp(null);

		assertEquals(2, this.ba.getBackPointerCapacity());
	}

	public void testInsertEnoughToUseMetablock() throws Exception {
		// We need enough instances to fill several full blocks since we don't reclaim
		// memory until there are two unused blocks.
		int numToAllocate = RawGrowableArray.getMaxGrowableBlockSize() * 4 + 1;
		
		List<ForwardPointerStruct> allocated = new ArrayList<>();

		for (int count = 0; count < numToAllocate; count++) {
			ForwardPointerStruct next = new ForwardPointerStruct(this.pdom);

			next.setBp(this.ba);
			assertEquals(next, this.ba.getBackPointer(count));
			allocated.add(next);
			assertEquals(count + 1, this.ba.backPointerSize());
		}

		assertEquals(allocated.get(numToAllocate - 1), this.ba.getBackPointer(numToAllocate - 1));
		assertEquals(numToAllocate, this.ba.backPointerSize());
		
		int correctSize = numToAllocate;
		for (ForwardPointerStruct next : allocated) {
			next.setBp(null);
			assertEquals(--correctSize, this.ba.backPointerSize());
		}

		assertEquals(0, this.ba.backPointerSize());
		assertEquals(2, this.ba.getBackPointerCapacity());
	}

	public void testGrowExistingMetablock() throws Exception {
		int blockSize = RawGrowableArray.getMaxGrowableBlockSize();

		this.ba.ensureBackPointerCapacity(2 * blockSize);

		assertEquals(2 * blockSize + 2, this.ba.getBackPointerCapacity());

		this.ba.ensureBackPointerCapacity(6 * blockSize);

		assertEquals(6 * blockSize + 2, this.ba.getBackPointerCapacity());
	}
}
