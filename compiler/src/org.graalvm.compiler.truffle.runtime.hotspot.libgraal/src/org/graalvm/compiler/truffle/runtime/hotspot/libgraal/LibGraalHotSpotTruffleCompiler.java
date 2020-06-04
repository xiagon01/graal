/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.truffle.runtime.hotspot.libgraal;

import static org.graalvm.libgraal.LibGraalScope.getIsolateThread;

import java.util.Map;

import org.graalvm.compiler.truffle.common.CompilableTruffleAST;
import org.graalvm.compiler.truffle.common.TruffleCompilation;
import org.graalvm.compiler.truffle.common.TruffleCompilationTask;
import org.graalvm.compiler.truffle.common.TruffleCompilerListener;
import org.graalvm.compiler.truffle.common.TruffleDebugContext;
import org.graalvm.compiler.truffle.common.TruffleInliningPlan;
import org.graalvm.compiler.truffle.common.hotspot.HotSpotTruffleCompiler;
import org.graalvm.libgraal.LibGraalObject;
import org.graalvm.libgraal.LibGraalScope;
import org.graalvm.util.OptionsEncoder;

/**
 * Encapsulates handles to {@link HotSpotTruffleCompiler} objects in the libgraal isolates.
 */
final class LibGraalHotSpotTruffleCompiler implements HotSpotTruffleCompiler {

    static final class Handle extends LibGraalObject {
        Handle(long handle) {
            super(handle);
        }
    }

    private final ThreadLocal<LibGraalTruffleCompilation> activeCompilation = new ThreadLocal<>();

    private final LibGraalTruffleRuntime runtime;

    private byte[] initialOptions;

    long handle() {
        if (initialOptions == null) {
            throw new IllegalStateException("Initial options are not yet initialized, missing call of the TruffleCompiler::initialized method.");
        }
        try (LibGraalScope scope = new LibGraalScope()) {
            return scope.getIsolate().getSingleton(Handle.class, () -> {
                long isolateThread = getIsolateThread();
                long compilerHandle = TruffleToLibGraalCalls.newCompiler(isolateThread, runtime.handle());
                Handle compiler = new Handle(compilerHandle);
                TruffleToLibGraalCalls.initializeCompiler(isolateThread, compilerHandle, initialOptions);
                return compiler;
            }).getHandle();
        }
    }

    LibGraalHotSpotTruffleCompiler(LibGraalTruffleRuntime runtime) {
        this.runtime = runtime;
    }

    @SuppressWarnings("try")
    @Override
    public void initialize(Map<String, Object> options) {
        this.initialOptions = OptionsEncoder.encode(options);
        // Force installation of the Truffle call boundary methods.
        // See AbstractHotSpotTruffleRuntime.setDontInlineCallBoundaryMethod
        // for further details.
        handle();
    }

    @Override
    public TruffleCompilation openCompilation(CompilableTruffleAST compilable) {
        LibGraalScope scope = new LibGraalScope();
        long compilationHandle = TruffleToLibGraalCalls.openCompilation(getIsolateThread(), handle(), compilable);
        LibGraalTruffleCompilation compilation = new LibGraalTruffleCompilation(this, compilationHandle, scope);
        activeCompilation.set(compilation);
        return compilation;
    }

    @Override
    public TruffleDebugContext openDebugContext(Map<String, Object> options, TruffleCompilation compilation) {
        return IgvSupport.create(this, options, (LibGraalTruffleCompilation) compilation);
    }

    @Override
    public void doCompile(TruffleDebugContext debug,
                    TruffleCompilation compilation,
                    Map<String, Object> options,
                    TruffleInliningPlan inlining,
                    TruffleCompilationTask task,
                    TruffleCompilerListener listener) {
        byte[] encodedOptions = OptionsEncoder.encode(options);
        long debugContextHandle = ((IgvSupport) debug).getHandle();
        long compilationHandle = ((LibGraalTruffleCompilation) compilation).getHandle();
        TruffleToLibGraalCalls.doCompile(getIsolateThread(), handle(), debugContextHandle, compilationHandle, encodedOptions, inlining, task, listener);
    }

    @SuppressWarnings("try")
    @Override
    public String getCompilerConfigurationName() {
        try (LibGraalScope scope = new LibGraalScope()) {
            return TruffleToLibGraalCalls.getCompilerConfigurationName(getIsolateThread(), handle());
        }
    }

    @SuppressWarnings("try")
    @Override
    public void shutdown() {
        try (LibGraalScope scope = new LibGraalScope()) {
            TruffleToLibGraalCalls.shutdown(getIsolateThread(), handle());
        }
    }

    @SuppressWarnings("try")
    @Override
    public void installTruffleCallBoundaryMethods() {
        try (LibGraalScope scope = new LibGraalScope()) {
            TruffleToLibGraalCalls.installTruffleCallBoundaryMethods(getIsolateThread(), handle());
        }
    }

    Integer pendingTransferToInterpreterOffset;

    @SuppressWarnings("try")
    @Override
    public int pendingTransferToInterpreterOffset() {
        if (pendingTransferToInterpreterOffset == null) {
            try (LibGraalScope scope = new LibGraalScope()) {
                pendingTransferToInterpreterOffset = TruffleToLibGraalCalls.pendingTransferToInterpreterOffset(getIsolateThread(), handle());
            }
        }
        return pendingTransferToInterpreterOffset;
    }

    void closeCompilation(LibGraalTruffleCompilation compilation) {
        assert activeCompilation.get() == compilation;
        activeCompilation.set(null);
    }

    LibGraalTruffleCompilation getActiveCompilation() {
        return activeCompilation.get();
    }
}
