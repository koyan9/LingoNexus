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

/**
 * Profiles that control how much execution metadata is emitted in {@link ScriptResult}.
 */
public enum ResultMetadataProfile {

    BASIC,
    TIMING,
    FULL;

    public boolean includesTimingDetails() {
        return this == TIMING || this == FULL;
    }

    public boolean includesDiagnosticDetails() {
        return this == FULL;
    }

    public static ResultMetadataProfile fromDetailedEnabled(boolean detailedResultMetadataEnabled) {
        return detailedResultMetadataEnabled ? FULL : BASIC;
    }

    public static ResultMetadataProfile fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return ResultMetadataProfile.valueOf(value.trim().toUpperCase());
    }
}
