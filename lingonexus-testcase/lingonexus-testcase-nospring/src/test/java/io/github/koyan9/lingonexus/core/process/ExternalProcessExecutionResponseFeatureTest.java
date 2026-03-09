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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Execution Response Feature Tests")
class ExternalProcessExecutionResponseFeatureTest {

    @Test
    @DisplayName("Should default null metadata and cache statistics to empty maps")
    void shouldDefaultNullMetadataAndCacheStatisticsToEmptyMaps() {
        ExternalProcessExecutionResponse response = new ExternalProcessExecutionResponse(
                true,
                "SUCCESS",
                Boolean.TRUE,
                null,
                null,
                0L,
                null
        );

        assertNotNull(response.getMetadata());
        assertNotNull(response.getExecutorCacheStatistics());
        assertTrue(response.getMetadata().isEmpty());
        assertTrue(response.getExecutorCacheStatistics().isEmpty());
    }
}
