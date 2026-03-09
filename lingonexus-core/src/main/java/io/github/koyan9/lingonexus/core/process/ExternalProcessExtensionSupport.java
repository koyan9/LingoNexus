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
package io.github.koyan9.lingonexus.core.process;

import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorConsumer;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;

/**
 * Shared helper for external-process extension descriptor export/import.
 */
public final class ExternalProcessExtensionSupport {

    private ExternalProcessExtensionSupport() {
    }

    public static ExternalProcessExtensionDescriptor createDescriptor(Object extension) {
        Map<String, Object> descriptor = Collections.emptyMap();
        if (extension instanceof ExternalProcessDescriptorProvider) {
            Map<String, Object> raw = ((ExternalProcessDescriptorProvider) extension).toExternalProcessDescriptor();
            descriptor = raw != null ? JsonSafeValueNormalizer.normalizeMap(raw) : Collections.<String, Object>emptyMap();
        }
        return new ExternalProcessExtensionDescriptor(extension.getClass().getName(), descriptor);
    }

    public static <T> T instantiate(ExternalProcessExtensionDescriptor descriptor, Class<T> expectedType) {
        try {
            Class<?> loadedClass = Class.forName(descriptor.getClassName(), true, Thread.currentThread().getContextClassLoader());
            if (!expectedType.isAssignableFrom(loadedClass)) {
                throw new IllegalStateException("Class is not assignable to " + expectedType.getName() + ": " + descriptor.getClassName());
            }

            Map<String, Object> state = descriptor.getDescriptor() != null ? descriptor.getDescriptor() : Collections.<String, Object>emptyMap();
            Method factory = findFactoryMethod(loadedClass, expectedType);
            if (factory != null) {
                return expectedType.cast(factory.invoke(null, state));
            }

            Constructor<?> constructor = loadedClass.getDeclaredConstructor();
            Object instance = constructor.newInstance();
            if (!state.isEmpty()) {
                if (instance instanceof ExternalProcessDescriptorConsumer) {
                    ((ExternalProcessDescriptorConsumer) instance).loadExternalProcessDescriptor(state);
                } else {
                    throw new IllegalStateException(
                            "Class does not support descriptor restoration without a static factory method: " + descriptor.getClassName()
                    );
                }
            }
            return expectedType.cast(instance);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate external-process extension: " + descriptor.getClassName(), e);
        }
    }

    public static void validateCompatibility(Class<?> type, Class<?> expectedType, String category, boolean hasDescriptorState) {
        if (!expectedType.isAssignableFrom(type)) {
            throw new IllegalStateException(category + " is not assignable to " + expectedType.getName() + ": " + type.getName());
        }
        if (type.isAnonymousClass() || type.isLocalClass() || type.isSynthetic()) {
            throw new IllegalStateException(
                    "EXTERNAL_PROCESS requires a named " + category + " class on the worker classpath: " + type.getName()
            );
        }

        if (findFactoryMethod(type, expectedType) != null) {
            return;
        }

        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            if (!Modifier.isPublic(type.getModifiers()) || !Modifier.isPublic(constructor.getModifiers())) {
                throw new IllegalStateException(
                        "EXTERNAL_PROCESS requires a public no-arg constructor for " + category + ": " + type.getName()
                );
            }
            if (hasDescriptorState && !ExternalProcessDescriptorConsumer.class.isAssignableFrom(type)) {
                throw new IllegalStateException(
                        "EXTERNAL_PROCESS descriptor state requires " + ExternalProcessDescriptorConsumer.class.getName() +
                                " or a static fromExternalProcessDescriptor(Map) factory: " + type.getName()
                );
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "EXTERNAL_PROCESS requires a public no-arg constructor or static fromExternalProcessDescriptor(Map) factory for " +
                            category + ": " + type.getName(),
                    e
            );
        }
    }

    private static Method findFactoryMethod(Class<?> type, Class<?> expectedType) {
        try {
            Method method = type.getMethod("fromExternalProcessDescriptor", Map.class);
            if (Modifier.isStatic(method.getModifiers()) && expectedType.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }
}
