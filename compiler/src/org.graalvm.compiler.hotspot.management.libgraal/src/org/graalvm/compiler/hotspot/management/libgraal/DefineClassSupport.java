/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.hotspot.management.libgraal;

import static org.graalvm.libgraal.jni.JNIUtil.GetMethodID;
import static org.graalvm.libgraal.jni.JNIUtil.GetStaticMethodID;
import static org.graalvm.nativeimage.c.type.CTypeConversion.toCString;
import static org.graalvm.word.WordFactory.nullPointer;

import org.graalvm.libgraal.jni.JNI;
import org.graalvm.libgraal.jni.JNIExceptionWrapper;
import org.graalvm.libgraal.jni.JNIUtil;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CTypeConversion.CCharPointerHolder;
import org.graalvm.word.WordFactory;

/**
 * Support calls for defining classes in a HotSpot class loader.
 */
final class DefineClassSupport {

    private static final String CLASS_SERVICES = "jdk/vm/ci/services/Services";

    private static final String[] METHOD_GET_JVMCI_CLASS_LOADER = {
                    "getJVMCIClassLoader",
                    "()Ljava/lang/ClassLoader;"
    };
    private static final String[] METHOD_GET_PLATFORM_CLASS_LOADER = {
                    "getPlatformClassLoader",
                    "()Ljava/lang/ClassLoader;"
    };
    private static final String[] METHOD_LOAD_CLASS = {
                    "loadClass",
                    "(Ljava/lang/String;)Ljava/lang/Class;"
    };

    private DefineClassSupport() {
    }

    static JNI.JObject getJVMCIClassLoader(JNI.JNIEnv env) {
        JNI.JClass clazz;
        try (CCharPointerHolder className = CTypeConversion.toCString(CLASS_SERVICES)) {
            clazz = JNIUtil.FindClass(env, className.get());
        }
        if (clazz.isNull()) {
            throw new InternalError("No such class " + CLASS_SERVICES);
        }
        JNI.JMethodID getClassLoaderId = findMethod(env, clazz, true, true, METHOD_GET_JVMCI_CLASS_LOADER);
        if (getClassLoaderId.isNonNull()) {
            return env.getFunctions().getCallStaticObjectMethodA().call(env, clazz, getClassLoaderId, nullPointer());
        }
        try (CCharPointerHolder className = CTypeConversion.toCString(JNIUtil.getBinaryName(ClassLoader.class.getName()))) {
            clazz = JNIUtil.FindClass(env, className.get());
        }
        if (clazz.isNull()) {
            throw new InternalError("No such class " + ClassLoader.class.getName());
        }
        getClassLoaderId = findMethod(env, clazz, true, true, METHOD_GET_PLATFORM_CLASS_LOADER);
        if (getClassLoaderId.isNonNull()) {
            return env.getFunctions().getCallStaticObjectMethodA().call(env, clazz, getClassLoaderId, nullPointer());
        }
        return WordFactory.nullPointer();
    }

    /**
     * Finds a class in HotSpot heap using a given {@code ClassLoader}.
     *
     * @param env the {@code JNIEnv}
     * @param className the class name
     */
    static JNI.JClass findClass(JNI.JNIEnv env, JNI.JObject classLoader, String className) {
        if (classLoader.isNull()) {
            throw new IllegalArgumentException("ClassLoader must be non null.");
        }
        JNI.JMethodID findClassId = findMethod(env, JNIUtil.GetObjectClass(env, classLoader), false, false, METHOD_LOAD_CLASS);
        JNI.JValue params = StackValue.get(1, JNI.JValue.class);
        params.addressOf(0).setJObject(JNIUtil.createHSString(env, className.replace('/', '.')));
        return (JNI.JClass) env.getFunctions().getCallObjectMethodA().call(env, classLoader, findClassId, params);
    }

    private static JNI.JMethodID findMethod(JNI.JNIEnv env, JNI.JClass clazz, boolean staticMethod, boolean optional, String[] descriptor) {
        assert descriptor.length == 2;
        JNI.JMethodID result;
        try (CCharPointerHolder name = toCString(descriptor[0]); CCharPointerHolder sig = toCString(descriptor[1])) {
            result = staticMethod ? GetStaticMethodID(env, clazz, name.get(), sig.get()) : GetMethodID(env, clazz, name.get(), sig.get());
            if (optional) {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env, NoSuchMethodError.class);
            } else {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env);
            }
            return result;
        }
    }
}
