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
package io.github.koyan9.lingonexus.api.result;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared helpers for built-in and custom metadata policy resolution.
 */
public final class ResultMetadataPolicySupport {

    private ResultMetadataPolicySupport() {
    }

    public static Map<String, ResultMetadataPolicyTemplate> normalizeTemplates(Map<String, ResultMetadataPolicyTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ResultMetadataPolicyTemplate> result = new LinkedHashMap<String, ResultMetadataPolicyTemplate>();
        for (Map.Entry<String, ResultMetadataPolicyTemplate> entry : templates.entrySet()) {
            ResultMetadataPolicyTemplate template = entry.getValue();
            if (template == null || template.getName() == null || template.getName().trim().isEmpty()) {
                continue;
            }
            result.put(normalizePolicyName(template.getName()), template);
        }
        return result.isEmpty() ? Collections.<String, ResultMetadataPolicyTemplate>emptyMap() : Collections.unmodifiableMap(result);
    }

    public static Map<String, ResultMetadataPolicyTemplate> mergeTemplates(Map<String, ResultMetadataPolicyTemplate> registryTemplates,
                                                                           Map<String, ResultMetadataPolicyTemplate> localTemplates) {
        if ((registryTemplates == null || registryTemplates.isEmpty())
                && (localTemplates == null || localTemplates.isEmpty())) {
            return Collections.emptyMap();
        }
        Map<String, ResultMetadataPolicyTemplate> merged = new LinkedHashMap<String, ResultMetadataPolicyTemplate>();
        if (registryTemplates != null && !registryTemplates.isEmpty()) {
            merged.putAll(normalizeTemplates(registryTemplates));
        }
        if (localTemplates != null && !localTemplates.isEmpty()) {
            merged.putAll(normalizeTemplates(localTemplates));
        }
        return merged.isEmpty() ? Collections.<String, ResultMetadataPolicyTemplate>emptyMap() : Collections.unmodifiableMap(merged);
    }

    public static Set<ResultMetadataCategory> resolveNamedPolicy(String policyName,
                                                                 Map<String, ResultMetadataPolicyTemplate> templates) {
        if (policyName == null || policyName.trim().isEmpty()) {
            return null;
        }
        ResultMetadataPolicy builtInPolicy = ResultMetadataPolicy.fromString(policyName);
        if (builtInPolicy != null) {
            return builtInPolicy.getCategories();
        }
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        return resolveTemplate(normalizePolicyName(policyName), templates, new LinkedHashSet<String>());
    }

    public static String normalizePolicyName(String policyName) {
        return policyName != null ? policyName.trim().toUpperCase() : null;
    }

    private static Set<ResultMetadataCategory> resolveTemplate(String normalizedPolicyName,
                                                               Map<String, ResultMetadataPolicyTemplate> templates,
                                                               Set<String> visited) {
        if (normalizedPolicyName == null) {
            return null;
        }
        if (!visited.add(normalizedPolicyName)) {
            throw new IllegalStateException("Circular result metadata policy inheritance detected: " + normalizedPolicyName);
        }
        ResultMetadataPolicyTemplate template = templates.get(normalizedPolicyName);
        if (template == null) {
            return null;
        }
        EnumSet<ResultMetadataCategory> resolved = EnumSet.noneOf(ResultMetadataCategory.class);
        Set<ResultMetadataCategory> inherited = resolveNamedPolicy(template.getParentPolicyName(), templates);
        if (inherited != null) {
            resolved.addAll(inherited);
        }
        if (template.getCategories() != null) {
            resolved.addAll(template.getCategories());
        }
        return resolved.isEmpty()
                ? Collections.<ResultMetadataCategory>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(resolved));
    }
}
