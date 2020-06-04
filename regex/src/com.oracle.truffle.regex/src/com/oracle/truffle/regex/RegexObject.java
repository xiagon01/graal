/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.NoMatchResult;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.runtime.nodes.ExpectStringOrTruffleObjectNode;
import com.oracle.truffle.regex.runtime.nodes.StringEqualsNode;
import com.oracle.truffle.regex.runtime.nodes.ToLongNode;
import com.oracle.truffle.regex.tregex.parser.flavors.PythonFlags;
import com.oracle.truffle.regex.util.TruffleNull;
import com.oracle.truffle.regex.util.TruffleReadOnlyKeysArray;
import com.oracle.truffle.regex.util.TruffleReadOnlyMap;

/**
 * {@link RegexObject} represents a compiled regular expression that can be used to match against
 * input strings. It is the result of executing a {@link RegexEngine}. It exposes the following
 * three properties:
 * <ol>
 * <li>{@link String} {@code pattern}: the source of the compiled regular expression</li>
 * <li>{@link TruffleObject} {@code flags}: the set of flags passed to the regular expression
 * compiler. The type differs based on the flavor of regular expressions used:
 * <ul>
 * <li>{@link RegexFlags} if the flavor was {@code ECMAScript}</li>
 * <li>{@link PythonFlags} if the flavor was {@code PythonStr} or {@code PythonBytes}</li>
 * </ul>
 * </li>
 * <li>{@code int groupCount}: number of capture groups present in the regular expression, including
 * group 0.</li>
 * <li>{@link RegexObjectExecMethod} {@code exec}: an executable method that matches the compiled
 * regular expression against a string. The method accepts two parameters:
 * <ol>
 * <li>{@link Object} {@code input}: the character sequence to search in. This may either be a
 * {@link String} or a {@link TruffleObject} that responds to
 * {@link InteropLibrary#hasArrayElements(Object)} and returns {@link Character}s on indexed
 * {@link InteropLibrary#readArrayElement(Object, long)} requests.</li>
 * <li>{@link Number} {@code fromIndex}: the position to start searching from. This argument will be
 * cast to {@code int}, since a {@link String} can not be longer than {@link Integer#MAX_VALUE}. If
 * {@code fromIndex} is greater than {@link Integer#MAX_VALUE}, this method will immediately return
 * NO_MATCH.</li>
 * </ol>
 * The return value is a {@link RegexResult}. The contents of the {@code exec} can be compiled
 * lazily and so its first invocation might involve a longer delay as the regular expression is
 * compiled on the fly.
 * </ol>
 * <p>
 */
@ExportLibrary(InteropLibrary.class)
public final class RegexObject extends AbstractConstantKeysObject {

    static final String PROP_EXEC = "exec";
    private static final String PROP_PATTERN = "pattern";
    private static final String PROP_FLAGS = "flags";
    private static final String PROP_GROUP_COUNT = "groupCount";
    private static final String PROP_GROUPS = "groups";
    private static final TruffleReadOnlyKeysArray KEYS = new TruffleReadOnlyKeysArray(PROP_EXEC, PROP_PATTERN, PROP_FLAGS, PROP_GROUP_COUNT, PROP_GROUPS);

    private final RegexCompiler compiler;
    private final RegexSource source;
    private final TruffleObject flags;
    private final int numberOfCaptureGroups;
    private final TruffleObject namedCaptureGroups;
    private Object compiledRegexObject;

    public RegexObject(RegexCompiler compiler, RegexSource source, TruffleObject flags, int numberOfCaptureGroups, Map<String, Integer> namedCaptureGroups) {
        this.compiler = compiler;
        this.source = source;
        this.flags = flags;
        this.numberOfCaptureGroups = numberOfCaptureGroups;
        this.namedCaptureGroups = namedCaptureGroups != null ? new TruffleReadOnlyMap(namedCaptureGroups) : TruffleNull.INSTANCE;
    }

    public RegexSource getSource() {
        return source;
    }

    public TruffleObject getFlags() {
        return flags;
    }

    public int getNumberOfCaptureGroups() {
        return numberOfCaptureGroups;
    }

    public TruffleObject getNamedCaptureGroups() {
        return namedCaptureGroups;
    }

    public Object getCompiledRegexObject() {
        if (compiledRegexObject == null) {
            compiledRegexObject = compileRegex();
        }
        return compiledRegexObject;
    }

    @TruffleBoundary
    private Object compileRegex() {
        return compiler.compile(source);
    }

    public void setCompiledRegexObject(TruffleObject compiledRegexObject) {
        this.compiledRegexObject = compiledRegexObject;
    }

    public RegexObjectExecMethod getExecMethod() {
        // this allocation should get virtualized and optimized away by graal
        return new RegexObjectExecMethod(this);
    }

    @Override
    public TruffleReadOnlyKeysArray getKeys() {
        return KEYS;
    }

    @Override
    public Object readMemberImpl(String symbol) throws UnknownIdentifierException {
        switch (symbol) {
            case PROP_EXEC:
                return getExecMethod();
            case PROP_PATTERN:
                return getSource().getPattern();
            case PROP_FLAGS:
                return getFlags();
            case PROP_GROUP_COUNT:
                return getNumberOfCaptureGroups();
            case PROP_GROUPS:
                return getNamedCaptureGroups();
            default:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw UnknownIdentifierException.create(symbol);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberInvocable(String member,
                    @Shared("isExecPropNode") @Cached StringEqualsNode isExecPropNode) {
        return isExecPropNode.execute(member, PROP_EXEC);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] args,
                    @Shared("isExecPropNode") @Cached StringEqualsNode isExecPropNode,
                    @Cached GetCompiledRegexNode getCompiledRegexNode,
                    @Cached ExpectStringOrTruffleObjectNode expectStringOrTruffleObjectNode,
                    @Cached ToLongNode toLongNode,
                    @Cached ExecCompiledRegexNode execNode) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (!isExecPropNode.execute(member, PROP_EXEC)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw UnknownIdentifierException.create(member);
        }
        if (args.length != 2) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw ArityException.create(2, args.length);
        }
        Object input = expectStringOrTruffleObjectNode.execute(args[0]);
        long fromIndex = toLongNode.execute(args[1]);
        if (fromIndex > Integer.MAX_VALUE) {
            return NoMatchResult.getInstance();
        }
        return execNode.execute(getCompiledRegexNode.execute(this), input, (int) fromIndex);
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class RegexObjectExecMethod extends AbstractRegexObject {

        private final RegexObject regex;

        public RegexObjectExecMethod(RegexObject regex) {
            this.regex = regex;
        }

        public RegexObject getRegexObject() {
            return regex;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @ExportMessage
        Object execute(Object[] args,
                        @Cached GetCompiledRegexNode getCompiledRegexNode,
                        @Cached ExpectStringOrTruffleObjectNode expectStringOrTruffleObjectNode,
                        @Cached ToLongNode toLongNode,
                        @Cached ExecCompiledRegexNode execNode) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (args.length != 2) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw ArityException.create(2, args.length);
            }
            Object input = expectStringOrTruffleObjectNode.execute(args[0]);
            long fromIndex = toLongNode.execute(args[1]);
            if (fromIndex > Integer.MAX_VALUE) {
                return NoMatchResult.getInstance();
            }
            try {
                return execNode.execute(getCompiledRegexNode.execute(getRegexObject()), input, (int) fromIndex);
            } catch (UnknownIdentifierException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RuntimeException(e);
            }
        }

    }

    @ReportPolymorphism
    @GenerateUncached
    abstract static class GetCompiledRegexNode extends Node {

        abstract Object execute(RegexObject receiver);

        @SuppressWarnings("unused")
        @Specialization(guards = "receiver == cachedReceiver", limit = "4")
        static Object executeFixed(RegexObject receiver,
                        @Cached("receiver") RegexObject cachedReceiver,
                        @Cached("receiver.getCompiledRegexObject()") Object cachedCompiledRegex) {
            return cachedCompiledRegex;
        }

        @Specialization(replaces = "executeFixed")
        static Object executeVarying(RegexObject receiver) {
            return receiver.getCompiledRegexObject();
        }
    }

    @ReportPolymorphism
    @ImportStatic(RegexObject.class)
    @GenerateUncached
    abstract static class ExecCompiledRegexNode extends Node {

        abstract Object execute(Object receiver, Object input, int fromIndex)
                        throws UnsupportedMessageException, ArityException, UnsupportedTypeException, UnknownIdentifierException;

        @SuppressWarnings("unused")
        @Specialization(guards = "receiver == cachedReceiver", limit = "4")
        static Object executeTRegexFixed(CompiledRegexObject receiver, Object input, int fromIndex,
                        @Cached("receiver") CompiledRegexObject cachedReceiver,
                        @Cached("create(cachedReceiver.getCallTarget())") DirectCallNode directCallNode) {
            return directCallNode.call(input, fromIndex);
        }

        @Specialization(replaces = "executeTRegexFixed")
        static Object executeTRegexVarying(CompiledRegexObject receiver, Object input, int fromIndex,
                        @Cached IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(receiver.getCallTarget(), input, fromIndex);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "receivers.isMemberInvocable(receiver, PROP_EXEC)", limit = "4")
        static Object executeForeign(TruffleObject receiver, Object input, int fromIndex,
                        @CachedLibrary("receiver") InteropLibrary receivers) throws UnsupportedMessageException, ArityException, UnsupportedTypeException, UnknownIdentifierException {
            return receivers.invokeMember(receiver, "exec", input, fromIndex);
        }
    }
}
