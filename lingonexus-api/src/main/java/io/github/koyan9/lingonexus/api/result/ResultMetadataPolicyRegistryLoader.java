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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads {@link ResultMetadataPolicyRegistry} instances from simple property sources.
 */
public final class ResultMetadataPolicyRegistryLoader {

    public static final String DEFAULT_PREFIX = "resultMetadataPolicies.";

    private ResultMetadataPolicyRegistryLoader() {
    }

    public static ResultMetadataPolicyRegistry load(Properties properties) {
        return load(properties, DEFAULT_PREFIX);
    }

    public static ResultMetadataPolicyRegistry load(Properties properties, String prefix) {
        if (properties == null || properties.isEmpty()) {
            return ResultMetadataPolicyRegistry.create();
        }
        Map<String, String> flattened = new LinkedHashMap<String, String>();
        for (String propertyName : properties.stringPropertyNames()) {
            flattened.put(propertyName, properties.getProperty(propertyName));
        }
        return load(flattened, prefix);
    }

    public static ResultMetadataPolicyRegistry load(Map<String, ?> properties) {
        return load(properties, DEFAULT_PREFIX);
    }

    public static ResultMetadataPolicyRegistry load(Map<String, ?> properties, String prefix) {
        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistry.create();
        if (properties == null || properties.isEmpty()) {
            return registry;
        }
        String effectivePrefix = prefix != null ? prefix : DEFAULT_PREFIX;
        Map<String, ResultMetadataPolicyTemplate.Builder> builders = new LinkedHashMap<String, ResultMetadataPolicyTemplate.Builder>();
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith(effectivePrefix)) {
                continue;
            }
            String suffix = key.substring(effectivePrefix.length());
            int separatorIndex = suffix.indexOf('.');
            if (separatorIndex <= 0 || separatorIndex >= suffix.length() - 1) {
                continue;
            }
            String templateName = suffix.substring(0, separatorIndex);
            String attributeName = suffix.substring(separatorIndex + 1);
            ResultMetadataPolicyTemplate.Builder builder = builders.computeIfAbsent(
                    templateName,
                    name -> ResultMetadataPolicyTemplate.builder().name(name)
            );
            Object rawValue = entry.getValue();
            if ("parent".equals(attributeName) || "parentPolicyName".equals(attributeName)) {
                builder.parentPolicyName(rawValue != null ? String.valueOf(rawValue) : null);
            } else if ("categories".equals(attributeName)) {
                builder.categories(parseCategories(rawValue));
            }
        }
        for (ResultMetadataPolicyTemplate.Builder builder : builders.values()) {
            registry.registerTemplate(builder.build());
        }
        return registry;
    }

    public static ResultMetadataPolicyRegistry load(InputStream inputStream) throws IOException {
        return load(inputStream, DEFAULT_PREFIX);
    }

    public static ResultMetadataPolicyRegistry load(InputStream inputStream, String prefix) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return load(properties, prefix);
    }

    private static java.util.Set<ResultMetadataCategory> parseCategories(Object value) {
        java.util.EnumSet<ResultMetadataCategory> categories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
        if (value == null) {
            return categories;
        }
        String[] parts = String.valueOf(value).split(",");
        for (String part : parts) {
            String candidate = part != null ? part.trim() : "";
            if (!candidate.isEmpty()) {
                categories.add(ResultMetadataCategory.valueOf(candidate.toUpperCase()));
            }
        }
        return categories;
    }
}
