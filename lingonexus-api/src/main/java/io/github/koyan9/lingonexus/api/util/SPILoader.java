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

package io.github.koyan9.lingonexus.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A custom SPI loader that supports instantiation of implementation classes with:
 * <p>
 * 1. No-arg constructor (compatible with standard ServiceLoader)
 * <p>
 * 2. Single-arg constructor
 * <p>
 * 3. Multi-arg constructor (auto-infer parameter types)
 * <p>
 * 4. Constructor with null arguments (manual parameter type specification required)
 * <br/>
 * This loader reuses the standard SPI configuration file format (META-INF/services/[fully qualified name of interface])
 * but overrides the instantiation logic to support parameterized constructors.
 *
 * @param <T> The type of the service interface to load
 */
public class SPILoader {

    // Prefix for SPI configuration files (standard SPI path)
    private static final String SPI_CONFIG_PREFIX = "META-INF/services/";

    public static <T> Set<T> load(Class<T> interfaceClass, Object... constructorArgs) {
        return load(interfaceClass, null, constructorArgs);
    }

    /**
     * Load implementation classes of the specified interface, auto-inferring constructor parameter types.
     * <p>
     * This method automatically infers parameter types from the actual argument values.
     * It cannot handle null arguments (since type cannot be inferred from null), use {@link #loadWithTypes(Class, Class[], Object[])} instead.
     * <p>
     * Compatibility: supports no-arg (pass empty args), single-arg, and multi-arg constructors.
     *
     * @param interfaceClass The service interface class to load implementations for
     * @param function Determine whether there is a cache externally, and directly use the value if it exists
     * @param constructorArgs Arguments to pass to the constructor (0 or more)
     * @return Set of instantiated implementation objects (non-null, may be empty)
     * @throws IllegalArgumentException If any constructor argument is null (type inference failed)
     * @throws RuntimeException If SPI config file reading or class instantiation fails
     */
    public static <T> Set<T> load(Class<T> interfaceClass, BiFunction<Class<?>, Supplier<T>, T> function, Object... constructorArgs) {
        Set<T> instances = new HashSet<>();
        String configFileName = SPI_CONFIG_PREFIX + interfaceClass.getName();

        // Step 1: Build parameter type array by inferring from actual arguments
        Class<?>[] parameterTypes = new Class<?>[constructorArgs.length];
        for (int i = 0; i < constructorArgs.length; i++) {
            Object arg = constructorArgs[i];
            // Reject null arguments (cannot infer type from null)
            if (arg == null) {
                throw new IllegalArgumentException("Constructor argument[" + i + "] is null - cannot infer parameter type automatically. " +
                        "Use loadWithTypes() to specify parameter types manually.");
            }
            // Get actual type of the argument
            parameterTypes[i] = arg.getClass();
            // Convert wrapper types to primitive types (e.g., Integer -> int) for constructor matching
            parameterTypes[i] = getPrimitiveTypeIfWrapper(parameterTypes[i]);
        }

        // Delegate to core loading logic with inferred parameter types
        instances = loadCore(interfaceClass, configFileName, parameterTypes, constructorArgs, function);
        return instances;
    }

    /**
     * Load implementation classes of the specified interface with manually specified constructor parameter types.
     * <p>
     * This method is designed for constructors with null arguments (since null cannot be used for type inference).
     * You must explicitly specify the parameter types that match the target constructor.
     *
     * @param interfaceClass The service interface class to load implementations for
     * @param parameterTypes Explicit parameter types of the target constructor (must match constructor signature exactly)
     * @param constructorArgs Arguments to pass to the constructor (length must match parameterTypes length)
     * @return Set of instantiated implementation objects (non-null, may be empty)
     * @throws IllegalArgumentException If parameterTypes and constructorArgs length mismatch
     * @throws RuntimeException If SPI config file reading or class instantiation fails
     */
    public static <T> Set<T> loadWithTypes(Class<T> interfaceClass, Class<?>[] parameterTypes, Object[] constructorArgs) {
        Set<T> instances = new HashSet<>();
        String configFileName = SPI_CONFIG_PREFIX + interfaceClass.getName();

        // Validate parameter type and argument length consistency
        if (parameterTypes == null || constructorArgs == null || parameterTypes.length != constructorArgs.length) {
            throw new IllegalArgumentException("Parameter types length (" + (parameterTypes == null ? 0 : parameterTypes.length) +
                    ") does not match constructor arguments length (" + (constructorArgs == null ? 0 : constructorArgs.length) + ")");
        }

        // Convert wrapper types to primitive types for better compatibility
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] != null) {
                parameterTypes[i] = getPrimitiveTypeIfWrapper(parameterTypes[i]);
            }
        }

        // Delegate to core loading logic with manual parameter types
        instances = loadCore(interfaceClass, configFileName, parameterTypes, constructorArgs, null);
        return instances;
    }

    /**
     * Core loading logic - shared by load() and loadWithTypes() methods.
     * Handles SPI config file reading, class loading, and constructor instantiation.
     *
     * @param interfaceClass Target service interface
     * @param configFileName Full path of SPI config file
     * @param parameterTypes Constructor parameter types (inferred or manual)
     * @param constructorArgs Constructor arguments
     * @return Set of instantiated implementation objects
     */
    private static <T> Set<T> loadCore(Class<T> interfaceClass, String configFileName, Class<?>[] parameterTypes, Object[] constructorArgs, BiFunction<Class<?>, Supplier<T>, T> function) {
        Set<T> instances = new HashSet<>();

        try {
            // Step 1: Get context class loader (compatible with web containers/modular projects)
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = interfaceClass.getClassLoader();
            }

            // Step 2: Read all SPI config files (supports multiple JARs with same SPI config)
            Enumeration<URL> urls = classLoader.getResources(configFileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream inputStream = url.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    String className;
                    // Read each line (each line is a full qualified class name of implementation)
                    while ((className = reader.readLine()) != null) {
                        className = className.trim();
                        // Skip empty lines and comment lines (start with #)
                        if (className.isEmpty() || className.startsWith("#")) {
                            continue;
                        }

                        // Step 3: Load and instantiate the implementation class
                        try {
                            // Load class with the context class loader
                            Class<?> implClass = Class.forName(className, false, classLoader);

                            // Validation:
                            // 1. Must be a subclass/implementation of the target interface
                            // 2. Cannot be an interface or abstract class
                            if (!interfaceClass.isAssignableFrom(implClass)
                                    || implClass.isInterface()
                                    || java.lang.reflect.Modifier.isAbstract(implClass.getModifiers())) {
                                continue;
                            }

                            T instance;
                            if (function != null) {
                                instance = function.apply(implClass, () -> createInstance(interfaceClass, implClass, parameterTypes, constructorArgs));
                            } else {
                                // Get the matched constructor and create instance
                                instance = createInstance(interfaceClass, implClass, parameterTypes, constructorArgs);
                            }
                            if (instance != null) {
                                instances.add(instance);
                            }

                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Class not found: " + className, e);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read SPI config file from URL: " + url, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to locate SPI config file: " + configFileName, e);
        }

        return instances;
    }

    private static <T> T createInstance(Class<T> interfaceClass, Class<?> implClass, Class<?>[] parameterTypes, Object[] constructorArgs) {
        try {
            return interfaceClass.cast(
                    implClass.getConstructor(parameterTypes).newInstance(constructorArgs)
            );
        } catch (NoSuchMethodException e) {
            // Detailed error message for constructor mismatch
            throw new RuntimeException(
                    "No matching constructor found for class: " + implClass.getName() + "\n" +
                            "Expected parameter types: " + getParameterTypeNames(parameterTypes) + "\n" +
                            "Please check constructor parameter count and type consistency.", e
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate implementation class: " + implClass.getName(), e);
        }
    }

    /**
     * Helper method: Get human-readable parameter type names for error messages.
     *
     * @param parameterTypes Array of parameter classes
     * @return Comma-separated type names (e.g., "SandboxConfig, String, long")
     */
    private static String getParameterTypeNames(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "no arguments";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            sb.append(type == null ? "null" : type.getSimpleName());
            if (i < parameterTypes.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Helper method: Convert wrapper types to their corresponding primitive types (for constructor matching).
     * <p>
     * Example: Integer.class -> int.class, Long.class -> long.class
     * If the input is not a wrapper type, return it as-is.
     *
     * @param clazz Input class (may be wrapper type)
     * @return Primitive type (if wrapper) or original class
     */
    private static Class<?> getPrimitiveTypeIfWrapper(Class<?> clazz) {
        if (clazz == Integer.class) return int.class;
        if (clazz == Long.class) return long.class;
        if (clazz == Boolean.class) return boolean.class;
        if (clazz == Byte.class) return byte.class;
        if (clazz == Short.class) return short.class;
        if (clazz == Float.class) return float.class;
        if (clazz == Double.class) return double.class;
        if (clazz == Character.class) return char.class;
        return clazz;
    }
}
