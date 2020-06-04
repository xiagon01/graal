/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.word.Pointer;
import org.graalvm.word.UnsignedWord;

import com.oracle.svm.core.MemoryWalker;
import com.oracle.svm.core.heap.ObjectVisitor;
import com.oracle.svm.core.hub.LayoutEncoding;
import com.oracle.svm.core.log.Log;
import com.oracle.svm.core.snippets.KnownIntrinsics;

public final class ImageHeapWalker {
    private static final MemoryWalker.NativeImageHeapRegionAccess<ImageHeapInfo> READ_ONLY_PRIMITIVE_WALKER = new ReadOnlyPrimitiveMemoryWalkerAccess();
    private static final MemoryWalker.NativeImageHeapRegionAccess<ImageHeapInfo> READ_ONLY_REFERENCE_WALKER = new ReadOnlyReferenceMemoryWalkerAccess();
    private static final MemoryWalker.NativeImageHeapRegionAccess<ImageHeapInfo> WRITABLE_PRIMITIVE_WALKER = new WritablePrimitiveMemoryWalkerAccess();
    private static final MemoryWalker.NativeImageHeapRegionAccess<ImageHeapInfo> WRITABLE_REFERENCE_WALKER = new WritableReferenceMemoryWalkerAccess();

    private ImageHeapWalker() {
    }

    public static boolean walkRegions(ImageHeapInfo heapInfo, MemoryWalker.Visitor visitor) {
        return visitor.visitNativeImageHeapRegion(heapInfo, READ_ONLY_PRIMITIVE_WALKER) &&
                        visitor.visitNativeImageHeapRegion(heapInfo, READ_ONLY_REFERENCE_WALKER) &&
                        visitor.visitNativeImageHeapRegion(heapInfo, WRITABLE_PRIMITIVE_WALKER) &&
                        visitor.visitNativeImageHeapRegion(heapInfo, WRITABLE_REFERENCE_WALKER);
    }

    public static boolean walkImageHeapObjects(ImageHeapInfo heapInfo, ObjectVisitor visitor) {
        return walkPartition(heapInfo.firstReadOnlyPrimitiveObject, heapInfo.lastReadOnlyPrimitiveObject, visitor) &&
                        walkPartition(heapInfo.firstReadOnlyReferenceObject, heapInfo.lastReadOnlyReferenceObject, visitor) &&
                        walkPartition(heapInfo.firstWritablePrimitiveObject, heapInfo.lastWritablePrimitiveObject, visitor) &&
                        walkPartition(heapInfo.firstWritableReferenceObject, heapInfo.lastWritableReferenceObject, visitor);
    }

    private static boolean walkPartition(Object firstObject, Object lastObject, ObjectVisitor visitor) {
        if (firstObject == null || lastObject == null) {
            assert firstObject == null && lastObject == null;
            return true;
        }
        Pointer firstPointer = Word.objectToUntrackedPointer(firstObject);
        Pointer lastPointer = Word.objectToUntrackedPointer(lastObject);
        Pointer current = firstPointer;
        while (current.belowOrEqual(lastPointer)) {
            Object currentObject = KnownIntrinsics.convertUnknownValue(current.toObject(), Object.class);
            if (!visitor.visitObject(currentObject)) {
                return false;
            }
            current = LayoutEncoding.getObjectEnd(currentObject);
        }
        return true;
    }

    static void logPartitionBoundaries(Log log, ImageHeapInfo imageHeapInfo) {
        log.string("ReadOnly Primitives: ").hex(Word.objectToUntrackedPointer(imageHeapInfo.firstReadOnlyPrimitiveObject)).string(" .. ").hex(
                        Word.objectToUntrackedPointer(imageHeapInfo.lastReadOnlyPrimitiveObject)).newline();
        log.string("ReadOnly References: ").hex(Word.objectToUntrackedPointer(imageHeapInfo.firstReadOnlyReferenceObject)).string(" .. ").hex(
                        Word.objectToUntrackedPointer(imageHeapInfo.lastReadOnlyReferenceObject)).newline();
        log.string("Writable Primitives: ").hex(Word.objectToUntrackedPointer(imageHeapInfo.firstWritablePrimitiveObject)).string(" .. ").hex(
                        Word.objectToUntrackedPointer(imageHeapInfo.lastWritablePrimitiveObject)).newline();
        log.string("Writable References: ").hex(Word.objectToUntrackedPointer(imageHeapInfo.firstWritableReferenceObject)).string(" .. ").hex(
                        Word.objectToUntrackedPointer(imageHeapInfo.lastWritableReferenceObject));
    }
}

abstract class MemoryWalkerAccessBase implements MemoryWalker.NativeImageHeapRegionAccess<ImageHeapInfo> {
    private final String regionName;
    private final boolean containsReferences;
    private final boolean isWritable;

    @Platforms(Platform.HOSTED_ONLY.class)
    MemoryWalkerAccessBase(String regionName, boolean containsReferences, boolean isWritable) {
        this.regionName = regionName;
        this.containsReferences = containsReferences;
        this.isWritable = isWritable;
    }

    @Override
    public UnsignedWord getStart(ImageHeapInfo info) {
        return Word.objectToUntrackedPointer(getFirstObject(info));
    }

    @Override
    public UnsignedWord getSize(ImageHeapInfo info) {
        Pointer firstStart = Word.objectToUntrackedPointer(getFirstObject(info));
        Pointer lastEnd = LayoutEncoding.getObjectEnd(getLastObject(info));
        return lastEnd.subtract(firstStart);
    }

    @Override
    public String getRegionName(ImageHeapInfo region) {
        return regionName;
    }

    @Override
    public boolean containsReferences(ImageHeapInfo region) {
        return containsReferences;
    }

    @Override
    public boolean isWritable(ImageHeapInfo region) {
        return isWritable;
    }

    protected abstract Object getFirstObject(ImageHeapInfo info);

    protected abstract Object getLastObject(ImageHeapInfo info);
}

final class ReadOnlyPrimitiveMemoryWalkerAccess extends MemoryWalkerAccessBase {
    @Platforms(Platform.HOSTED_ONLY.class)
    ReadOnlyPrimitiveMemoryWalkerAccess() {
        super("read-only primitives", false, false);
    }

    @Override
    public Object getFirstObject(ImageHeapInfo info) {
        return info.firstReadOnlyPrimitiveObject;
    }

    @Override
    public Object getLastObject(ImageHeapInfo info) {
        return info.lastReadOnlyPrimitiveObject;
    }
}

final class ReadOnlyReferenceMemoryWalkerAccess extends MemoryWalkerAccessBase {
    @Platforms(Platform.HOSTED_ONLY.class)
    ReadOnlyReferenceMemoryWalkerAccess() {
        super("read-only references", true, false);
    }

    @Override
    public Object getFirstObject(ImageHeapInfo info) {
        return info.firstReadOnlyReferenceObject;
    }

    @Override
    public Object getLastObject(ImageHeapInfo info) {
        return info.lastReadOnlyReferenceObject;
    }
}

final class WritablePrimitiveMemoryWalkerAccess extends MemoryWalkerAccessBase {
    @Platforms(Platform.HOSTED_ONLY.class)
    WritablePrimitiveMemoryWalkerAccess() {
        super("writable primitives", false, true);
    }

    @Override
    public Object getFirstObject(ImageHeapInfo info) {
        return info.firstWritablePrimitiveObject;
    }

    @Override
    public Object getLastObject(ImageHeapInfo info) {
        return info.lastWritablePrimitiveObject;
    }
}

final class WritableReferenceMemoryWalkerAccess extends MemoryWalkerAccessBase {
    @Platforms(Platform.HOSTED_ONLY.class)
    WritableReferenceMemoryWalkerAccess() {
        super("writable references", true, true);
    }

    @Override
    public Object getFirstObject(ImageHeapInfo info) {
        return info.firstWritableReferenceObject;
    }

    @Override
    public Object getLastObject(ImageHeapInfo info) {
        return info.lastWritableReferenceObject;
    }
}
