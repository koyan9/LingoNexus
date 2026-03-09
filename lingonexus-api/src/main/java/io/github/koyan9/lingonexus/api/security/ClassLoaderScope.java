/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.koyan9.lingonexus.api.security;

import java.util.concurrent.Callable;

/**
 * Utility for executing code with a temporary context ClassLoader.
 */
public final class ClassLoaderScope {

    private ClassLoaderScope() {
    }

    public static <T> T call(ClassLoader contextClassLoader, Callable<T> callable) throws Exception {
        if (contextClassLoader == null) {
            return callable.call();
        }

        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(contextClassLoader);
        try {
            return callable.call();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    public static void run(ClassLoader contextClassLoader, Runnable runnable) {
        if (contextClassLoader == null) {
            runnable.run();
            return;
        }

        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(contextClassLoader);
        try {
            runnable.run();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }
}
