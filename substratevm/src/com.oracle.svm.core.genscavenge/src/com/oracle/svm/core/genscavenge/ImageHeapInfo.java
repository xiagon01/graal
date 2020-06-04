/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.core.genscavenge;

import org.graalvm.compiler.word.Word;
import org.graalvm.word.Pointer;

import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.annotate.Uninterruptible;
import com.oracle.svm.core.annotate.UnknownObjectField;

/**
 * Information on the multiple partitions that make up the image heap, which don't necessarily form
 * a contiguous block of memory (there can be holes in between), and their boundaries.
 */
public final class ImageHeapInfo {
    @UnknownObjectField(types = Object.class) public Object firstReadOnlyPrimitiveObject;
    @UnknownObjectField(types = Object.class) public Object lastReadOnlyPrimitiveObject;

    @UnknownObjectField(types = Object.class) public Object firstReadOnlyReferenceObject;
    @UnknownObjectField(types = Object.class) public Object lastReadOnlyReferenceObject;

    @UnknownObjectField(types = Object.class) public Object firstWritablePrimitiveObject;
    @UnknownObjectField(types = Object.class) public Object lastWritablePrimitiveObject;

    @UnknownObjectField(types = Object.class) public Object firstWritableReferenceObject;
    @UnknownObjectField(types = Object.class) public Object lastWritableReferenceObject;

    @UnknownObjectField(types = Object.class) public Object firstReadOnlyObject;
    @UnknownObjectField(types = Object.class) public Object lastReadOnlyObject;

    @UnknownObjectField(types = Object.class) public Object firstWritableObject;
    @UnknownObjectField(types = Object.class) public Object lastWritableObject;

    @UnknownObjectField(types = Object.class) public Object firstObject;
    @UnknownObjectField(types = Object.class) public Object lastObject;

    public ImageHeapInfo() {
    }

    @SuppressWarnings("hiding")
    public void initialize(Object firstReadOnlyPrimitiveObject, Object lastReadOnlyPrimitiveObject, Object firstReadOnlyReferenceObject, Object lastReadOnlyReferenceObject,
                    Object firstWritablePrimitiveObject, Object lastWritablePrimitiveObject, Object firstWritableReferenceObject, Object lastWritableReferenceObject) {
        this.firstReadOnlyPrimitiveObject = firstReadOnlyPrimitiveObject;
        this.lastReadOnlyPrimitiveObject = lastReadOnlyPrimitiveObject;
        this.firstReadOnlyReferenceObject = firstReadOnlyReferenceObject;
        this.lastReadOnlyReferenceObject = lastReadOnlyReferenceObject;
        this.firstWritablePrimitiveObject = firstWritablePrimitiveObject;
        this.lastWritablePrimitiveObject = lastWritablePrimitiveObject;
        this.firstWritableReferenceObject = firstWritableReferenceObject;
        this.lastWritableReferenceObject = lastWritableReferenceObject;

        // Compute boundaries for checks considering partitions can be empty (first == last == null)
        this.firstReadOnlyObject = (firstReadOnlyPrimitiveObject != null) ? firstReadOnlyPrimitiveObject : firstReadOnlyReferenceObject;
        this.lastReadOnlyObject = (lastReadOnlyReferenceObject != null) ? lastReadOnlyReferenceObject : lastReadOnlyPrimitiveObject;
        this.firstWritableObject = (firstWritablePrimitiveObject != null) ? firstWritablePrimitiveObject : firstWritableReferenceObject;
        this.lastWritableObject = (lastWritableReferenceObject != null) ? lastWritableReferenceObject : lastWritablePrimitiveObject;
        this.firstObject = (firstReadOnlyObject != null) ? firstReadOnlyObject : firstWritableObject;
        this.lastObject = (lastWritableObject != null) ? lastWritableObject : lastReadOnlyObject;
    }

    /*
     * Convenience methods for asking if a Pointer is in the various native image heap partitions.
     *
     * These test [first .. last] rather than [first .. last), because last is in the partition.
     * These do not test for Pointers *into* the last object in each partition, though methods would
     * be easy to write, but slower.
     */

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInReadOnlyPrimitivePartition(Pointer ptr) {
        assert ptr.isNonNull();
        return Word.objectToUntrackedPointer(firstReadOnlyPrimitiveObject).belowOrEqual(ptr) && ptr.belowOrEqual(Word.objectToUntrackedPointer(lastReadOnlyPrimitiveObject));
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInWritablePrimitivePartition(Pointer ptr) {
        assert ptr.isNonNull();
        return Word.objectToUntrackedPointer(firstWritablePrimitiveObject).belowOrEqual(ptr) && ptr.belowOrEqual(Word.objectToUntrackedPointer(lastWritablePrimitiveObject));
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInReadOnlyReferencePartition(Pointer ptr) {
        assert ptr.isNonNull();
        return Word.objectToUntrackedPointer(firstReadOnlyReferenceObject).belowOrEqual(ptr) && ptr.belowOrEqual(Word.objectToUntrackedPointer(lastReadOnlyReferenceObject));
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInWritableReferencePartition(Pointer ptr) {
        assert ptr.isNonNull();
        return Word.objectToUntrackedPointer(firstWritableReferenceObject).belowOrEqual(ptr) && ptr.belowOrEqual(Word.objectToUntrackedPointer(lastWritableReferenceObject));
    }

    /**
     * This method only returns the correct result for pointers that point to the the start of an
     * object. This is sufficient for all our current use cases. This code must be as fast as
     * possible at the GC uses it for every visited reference.
     */
    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInImageHeap(Pointer objectPointer) {
        boolean result;
        if (objectPointer.isNull()) {
            result = false;
        } else if (SubstrateOptions.SpawnIsolates.getValue()) {
            result = objectPointer.aboveOrEqual(Word.objectToUntrackedPointer(firstObject)) && objectPointer.belowOrEqual(Word.objectToUntrackedPointer(lastObject));
        } else {
            result = objectPointer.aboveOrEqual(Word.objectToUntrackedPointer(firstReadOnlyObject)) &&
                            objectPointer.belowOrEqual(Word.objectToUntrackedPointer(lastReadOnlyObject)) ||
                            objectPointer.aboveOrEqual(Word.objectToUntrackedPointer(firstWritableObject)) &&
                                            objectPointer.belowOrEqual(Word.objectToUntrackedPointer(lastWritableObject));
        }

        assert result == isInImageHeapSlow(objectPointer);
        return result;
    }

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public boolean isInImageHeapSlow(Pointer objectPointer) {
        boolean result = false;
        if (objectPointer.isNonNull()) {
            result |= isInReadOnlyPrimitivePartition(objectPointer);
            result |= isInReadOnlyReferencePartition(objectPointer);
            result |= isInWritablePrimitivePartition(objectPointer);
            result |= isInWritableReferencePartition(objectPointer);
        }
        return result;
    }
}
