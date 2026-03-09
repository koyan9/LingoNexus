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

import io.github.koyan9.lingonexus.api.exception.ScriptSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Custom ClassLoader that restricts class loading based on whitelist/blacklist
 *
 * <p>This ClassLoader enforces class access control at runtime by checking
 * each class load request against configured blacklist, allowed patterns, and whitelist.</p>
 *
 * <p>Class loading priority:</p>
 * <ol>
 *   <li>Blacklist check (highest priority) - denied classes</li>
 *   <li>Allowed patterns check - classes required for script engine to function</li>
 *   <li>Whitelist check - user-defined allowed classes (if whitelist is not empty)</li>
 * </ol>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Blacklist has highest priority</li>
 *   <li>Allowed patterns are for framework/engine classes, not subject to whitelist restrictions</li>
 *   <li>Supports wildcard patterns: * (single level) and ** (multiple levels)</li>
 *   <li>Throws ScriptSecurityException for denied classes</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-11
 */
public class RestrictedClassLoader extends ClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(RestrictedClassLoader.class);

    private final boolean enabled;
    private final Set<String> whitelist;
    private final Set<String> blacklist;
    private final Set<String> allowedPatterns;

    /**
     * Create a RestrictedClassLoader with specified parent ClassLoader
     *
     * @param enabled         Whether security checks are enabled (if false, all checks are bypassed)
     * @param whitelist       Set of user-defined allowed class patterns
     * @param blacklist       Set of denied class patterns
     * @param allowedPatterns Set of framework/engine class patterns that are always allowed
     * @param parent          Parent ClassLoader
     */
    public RestrictedClassLoader(boolean enabled, Set<String> whitelist, Set<String> blacklist,
                                  Set<String> allowedPatterns, ClassLoader parent) {
        super(parent);
        this.enabled = enabled;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
        this.allowedPatterns = allowedPatterns;

        logger.debug("RestrictedClassLoader created with parent: {} (class: {}), enabled: {}, " +
                "whitelist patterns: {}, blacklist patterns: {}, allowed patterns: {}",
            parent, parent != null ? parent.getClass().getName() : "null", enabled,
            whitelist != null ? whitelist.size() : 0,
            blacklist != null ? blacklist.size() : 0,
            allowedPatterns != null ? allowedPatterns.size() : 0);
    }

    /**
     * Backward compatibility constructor without enabled parameter (defaults to true)
     *
     * @param whitelist       Set of user-defined allowed class patterns
     * @param blacklist       Set of denied class patterns
     * @param allowedPatterns Set of framework/engine class patterns that are always allowed
     * @param parent          Parent ClassLoader
     */
    public RestrictedClassLoader(Set<String> whitelist, Set<String> blacklist,
                                  Set<String> allowedPatterns, ClassLoader parent) {
        this(true, whitelist, blacklist, allowedPatterns, parent);
    }

    /**
     * Backward compatibility constructor without allowed patterns
     *
     * @param whitelist Set of allowed class patterns
     * @param blacklist Set of denied class patterns
     * @param parent    Parent ClassLoader
     */
    public RestrictedClassLoader(Set<String> whitelist, Set<String> blacklist, ClassLoader parent) {
        this(true, whitelist, blacklist, null, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // If security checks are disabled, bypass all restrictions
        if (!enabled) {
            logger.trace("Security checks disabled, loading class without restrictions: {}", name);
            return super.loadClass(name, resolve);
        }

        // Priority 1: Check blacklist first (highest priority - always deny)
        if (blacklist != null && !blacklist.isEmpty()) {
            for (String blackPattern : blacklist) {
                if (matchesPattern(name, blackPattern)) {
                    logger.debug("Class loading denied by blacklist: {}", name);
                    throw new ScriptSecurityException("Class access denied by blacklist: " + name);
                }
            }
        }

        // Priority 2: Check allowed patterns (framework/engine classes - always allow)
        // These are classes required for the script engine to function properly
        if (allowedPatterns != null && !allowedPatterns.isEmpty()) {
            for (String allowedPattern : allowedPatterns) {
                if (matchesPattern(name, allowedPattern)) {
                    logger.trace("Class loading allowed by framework pattern: {} (pattern: {})", name, allowedPattern);
                    return super.loadClass(name, resolve);
                }
            }
        }

        // Priority 3: Check whitelist (user-defined allowed classes)
        // Only enforce whitelist if it's not empty
        if (whitelist != null && !whitelist.isEmpty()) {
            boolean allowed = false;
            for (String whitePattern : whitelist) {
                if (matchesPattern(name, whitePattern)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                logger.error("Class loading denied - not in whitelist: {} (whitelist patterns: {})",
                    name, whitelist);
                throw new ScriptSecurityException("Class not in whitelist: " + name);
            }
        }

        // Load the class using parent classloader
        return super.loadClass(name, resolve);
    }

    /**
     * Match class name against pattern with wildcard support
     * <p>Examples:</p>
     * <ul>
     *   <li>"java.util.*" matches "java.util.List"</li>
     *   <li>"java.util.**" matches "java.util.concurrent.ConcurrentHashMap"</li>
     *   <li>"java.io.File" matches exactly "java.io.File"</li>
     *   <li>"Script$*" matches "Script$1", "Script$main", etc.</li>
     *   <li>"Line_*" matches "Line_1", "Line_2", etc.</li>
     * </ul>
     *
     * @param className The class name to check
     * @param pattern   The pattern to match against
     * @return true if the class name matches the pattern
     */
    private boolean matchesPattern(String className, String pattern) {
        if (pattern.equals(className)) {
            return true;
        }

        // Handle "**" wildcard (matches any number of package levels)
        if (pattern.endsWith(".**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return className.startsWith(prefix + ".");
        }

        // Handle ".*" wildcard (matches single package level)
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            // Check if className starts with prefix and has no additional dots
            if (className.startsWith(prefix + ".")) {
                String suffix = className.substring(prefix.length() + 1);
                return !suffix.contains(".");
            }
        }

        // Handle "*" wildcard at the end (matches any suffix)
        // Examples: "Script$*" matches "Script$1", "Line_*" matches "Line_1"
        if (pattern.endsWith("*") && !pattern.endsWith(".*") && !pattern.endsWith(".**")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return className.startsWith(prefix);
        }

        return false;
    }
}
