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
 * Categories of result metadata that can be enabled independently.
 */
public enum ResultMetadataCategory {

    TIMING,
    THREAD,
    MODULE,
    SECURITY,
    ERROR_DIAGNOSTICS;

    private static final Set<ResultMetadataCategory> BASIC_CATEGORIES = Collections.emptySet();
    private static final Set<ResultMetadataCategory> TIMING_CATEGORIES =
            Collections.unmodifiableSet(EnumSet.of(TIMING));
    private static final Set<ResultMetadataCategory> FULL_CATEGORIES =
            Collections.unmodifiableSet(EnumSet.allOf(ResultMetadataCategory.class));

    public static Set<ResultMetadataCategory> forProfile(ResultMetadataProfile profile) {
        if (profile == null || profile == ResultMetadataProfile.BASIC) {
            return BASIC_CATEGORIES;
        }
        if (profile == ResultMetadataProfile.TIMING) {
            return TIMING_CATEGORIES;
        }
        return FULL_CATEGORIES;
    }
}
