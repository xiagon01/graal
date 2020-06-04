/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.truffle.test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import org.graalvm.compiler.truffle.options.PolyglotCompilerOptions;
import org.graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import org.graalvm.compiler.truffle.test.nodes.RootTestNode;
import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class CompilationLoggingTest extends TestWithPolyglotOptions {

    @Test
    public void testCompilationSuccessTracingOff() {
        testHelper(
                        () -> RootNode.createConstantNode(true),
                        Collections.emptyMap(),
                        Arrays.asList(),
                        Arrays.asList("opt done", "opt queued", "opt start", "opt failed"));
    }

    @Test
    public void testCompilationSuccessTracingOn() {
        testHelper(
                        () -> RootNode.createConstantNode(true),
                        Collections.singletonMap("engine.TraceCompilation", "true"),
                        Arrays.asList("opt done"),
                        Arrays.asList("opt queued", "opt start", "opt failed"));
    }

    @Test
    public void testCompilationSuccessTracingDetails() {
        testHelper(
                        () -> RootNode.createConstantNode(true),
                        Collections.singletonMap("engine.TraceCompilationDetails", "true"),
                        Arrays.asList("opt queued", "opt start", "opt done"),
                        Arrays.asList("opt failed"));
    }

    @Test
    public void testCompilationFailureTracingOff() {
        testHelper(
                        CompilationLoggingTest::createFailureNode,
                        Collections.emptyMap(),
                        Arrays.asList(),
                        Arrays.asList("opt done", "opt queued", "opt start", "opt failed"));
    }

    @Test
    public void testCompilationFailureTracingOn() {
        testHelper(
                        CompilationLoggingTest::createFailureNode,
                        Collections.singletonMap("engine.TraceCompilation", "true"),
                        Arrays.asList("opt failed"),
                        Arrays.asList("opt queued", "opt start", "opt done"));
    }

    @Test
    public void testCompilationFailureTracingDetails() {
        testHelper(
                        CompilationLoggingTest::createFailureNode,
                        Collections.singletonMap("engine.TraceCompilationDetails", "true"),
                        Arrays.asList("opt queued", "opt start", "opt failed"),
                        Arrays.asList("opt done"));
    }

    @Test
    public void testExceptionFromPublish() {
        testHelper(
                        () -> RootNode.createConstantNode(true),
                        Collections.singletonMap("engine.TraceCompilationDetails", "true"),
                        Arrays.asList("opt start", "opt done"),
                        Collections.singletonList("opt failed"),
                        (lr) -> {
                            if (lr.getMessage().startsWith("opt start")) {
                                throw new RuntimeException();
                            }
                        });
    }

    private void testHelper(Supplier<RootNode> rootProvider, Map<String, String> additionalOptions, List<String> expected, List<String> unexpected) {
        testHelper(rootProvider, additionalOptions, expected, unexpected, null);
    }

    private void testHelper(Supplier<RootNode> rootProvider, Map<String, String> additionalOptions, List<String> expected, List<String> unexpected, Consumer<LogRecord> onPublishAction) {
        Assume.assumeFalse(hasTracingEnabled(rootProvider));
        TestHandler.Builder builder = TestHandler.newBuilder().onPublish(onPublishAction);
        for (String s : expected) {
            builder.expect(s);
        }
        for (String s : unexpected) {
            builder.ban(s);
        }
        TestHandler handler = builder.build();
        setupContext(newContextBuilder(additionalOptions, handler));
        OptimizedCallTarget warmUpTarget = (OptimizedCallTarget) Truffle.getRuntime().createCallTarget(rootProvider.get());
        warmUpTarget.call();
        handler.start();
        OptimizedCallTarget target = (OptimizedCallTarget) Truffle.getRuntime().createCallTarget(rootProvider.get());
        target.call();
        handler.assertLogs();
    }

    private static Context.Builder newContextBuilder(Map<String, String> additionalOptions, Handler handler) {
        Context.Builder builder = Context.newBuilder().option("engine.BackgroundCompilation", "false").option("engine.CompileImmediately", "true").allowAllAccess(true).allowExperimentalOptions(
                        true).logHandler(handler);
        for (Map.Entry<String, String> e : additionalOptions.entrySet()) {
            builder.option(e.getKey(), e.getValue());
        }
        return builder;
    }

    private boolean hasTracingEnabled(Supplier<RootNode> rootProvider) {
        setupContext();
        OptimizedCallTarget target = (OptimizedCallTarget) Truffle.getRuntime().createCallTarget(rootProvider.get());
        return target.getOptionValue(PolyglotCompilerOptions.TraceCompilation) || target.getOptionValue(PolyglotCompilerOptions.TraceCompilationDetails);
    }

    private static final class TestHandler extends Handler {

        private enum State {
            NEW,
            ACTIVE,
            DISPOSED
        }

        private final List<Pattern> expected;
        private final List<Pattern> unexpected;
        private final List<Pattern> failedUnexpected;
        private final List<LogEntry> allEvents;
        private final Consumer<LogRecord> onPublishAction;
        private volatile State state;

        private TestHandler(List<Pattern> expected, List<Pattern> unexpected, Consumer<LogRecord> onPublishAction) {
            this.expected = expected;
            this.unexpected = unexpected;
            this.onPublishAction = onPublishAction;
            this.failedUnexpected = new ArrayList<>();
            this.allEvents = new ArrayList<>();
            this.state = State.NEW;
        }

        @Override
        public synchronized void publish(LogRecord lr) {
            allEvents.add(new LogEntry(Thread.currentThread().getId(), state, lr.getMessage()));
            switch (state) {
                case NEW:
                    return;
                case ACTIVE:
                    break;
                case DISPOSED:
                    throw new IllegalStateException("Already closed");
                default:
                    throw new IllegalStateException("Unknown state " + state);
            }
            try {
                for (Iterator<Pattern> it = expected.iterator(); it.hasNext();) {
                    Pattern p = it.next();
                    if (p.matcher(lr.getMessage()).matches()) {
                        it.remove();
                        return;
                    }
                }
                for (Pattern p : unexpected) {
                    if (p.matcher(lr.getMessage()).matches()) {
                        failedUnexpected.add(p);
                        return;
                    }
                }
            } finally {
                if (onPublishAction != null) {
                    onPublishAction.accept(lr);
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        synchronized void start() {
            state = State.ACTIVE;
        }

        synchronized void assertLogs() {
            state = State.DISPOSED;
            StringBuilder sb = new StringBuilder();
            if (!expected.isEmpty()) {
                sb.append("Missing expected log records:\n");
                for (Pattern p : expected) {
                    sb.append(p.toString()).append("\n");
                }
            }
            if (!failedUnexpected.isEmpty()) {
                sb.append("Found un-expected log records:\n");
                for (Pattern p : failedUnexpected) {
                    sb.append(p.toString()).append("\n");
                }
            }
            if (sb.length() > 0) {
                sb.append("All log records:\n");
                for (LogEntry entry : allEvents) {
                    sb.append(entry).append("\n");
                }
                Assert.fail(sb.toString());
            }
        }

        static Builder newBuilder() {
            return new Builder();
        }

        static final class Builder {
            private final List<Pattern> expected;
            private final List<Pattern> unexpected;
            private Consumer<LogRecord> onPublishAction;

            private Builder() {
                expected = new LinkedList<>();
                unexpected = new LinkedList<>();
            }

            Builder expect(String substring) {
                return expect(toPattern(substring));
            }

            Builder expect(Pattern pattern) {
                expected.add(pattern);
                return this;
            }

            Builder ban(String substring) {
                return ban(toPattern(substring));
            }

            Builder ban(Pattern pattern) {
                unexpected.add(pattern);
                return this;
            }

            Builder onPublish(Consumer<LogRecord> action) {
                onPublishAction = action;
                return this;
            }

            TestHandler build() {
                return new TestHandler(expected, unexpected, onPublishAction);
            }

            private static Pattern toPattern(String substring) {
                return Pattern.compile(".*" + Pattern.quote(substring) + ".*");
            }
        }

        private static final class LogEntry {
            private final long threadId;
            private final State state;
            private final String message;

            LogEntry(long threadId, State state, String message) {
                this.threadId = threadId;
                this.state = state;
                this.message = message;
            }

            @Override
            public String toString() {
                return String.format("Thread %d, State: %s, Message: %s", threadId, state, message);
            }
        }
    }

    private static RootNode createFailureNode() {
        CompilerAssertsTest.NeverPartOfCompilationTestNode result = new CompilerAssertsTest.NeverPartOfCompilationTestNode();
        return new RootTestNode(new FrameDescriptor(), "neverPartOfCompilation", result);
    }
}
