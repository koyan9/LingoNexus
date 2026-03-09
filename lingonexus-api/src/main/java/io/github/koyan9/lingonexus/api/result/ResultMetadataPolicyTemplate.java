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
import java.util.Set;

/**
 * Custom named metadata policy template with optional parent inheritance.
 */
public class ResultMetadataPolicyTemplate {

    private final String name;
    private final String parentPolicyName;
    private final Set<ResultMetadataCategory> categories;

    private ResultMetadataPolicyTemplate(Builder builder) {
        this.name = builder.name;
        this.parentPolicyName = builder.parentPolicyName;
        this.categories = builder.categories.isEmpty()
                ? Collections.<ResultMetadataCategory>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.categories));
    }

    public String getName() {
        return name;
    }

    public String getParentPolicyName() {
        return parentPolicyName;
    }

    public Set<ResultMetadataCategory> getCategories() {
        return categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String parentPolicyName;
        private final Set<ResultMetadataCategory> categories = EnumSet.noneOf(ResultMetadataCategory.class);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parentPolicyName(String parentPolicyName) {
            this.parentPolicyName = parentPolicyName;
            return this;
        }

        public Builder categories(Set<ResultMetadataCategory> categories) {
            this.categories.clear();
            if (categories != null) {
                this.categories.addAll(categories);
            }
            return this;
        }

        public Builder category(ResultMetadataCategory... categories) {
            if (categories != null) {
                for (ResultMetadataCategory category : categories) {
                    if (category != null) {
                        this.categories.add(category);
                    }
                }
            }
            return this;
        }

        public ResultMetadataPolicyTemplate build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Result metadata policy template name cannot be blank");
            }
            return new ResultMetadataPolicyTemplate(this);
        }
    }
}
