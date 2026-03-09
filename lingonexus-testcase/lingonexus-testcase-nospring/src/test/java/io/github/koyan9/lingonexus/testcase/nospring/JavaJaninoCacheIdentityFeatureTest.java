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
package io.github.koyan9.lingonexus.testcase.nospring;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Java Janino Cache Identity Feature Tests")
class JavaJaninoCacheIdentityFeatureTest {

    @Test
    @DisplayName("Should keep separate cache entries for the same Java script when context types differ")
    void shouldKeepSeparateCacheEntriesForTheSameJavaScriptWhenContextTypesDiffer() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.JAVA)
                        .allowedSandboxLanguage(ScriptLanguage.JAVA)
                        .cacheConfig(CacheConfig.builder().enabled(true).build())
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(0L)
                                .isolationMode(ExecutionIsolationMode.DIRECT)
                                .build())
                        .build()
        );

        try {
            String script = "value != null";

            ScriptResult integerResult1 = executor.execute(script, "java", ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(1))));
            ScriptResult stringResult = executor.execute(script, "java", ScriptContext.of(Collections.<String, Object>singletonMap("value", "hello")));
            ScriptResult integerResult2 = executor.execute(script, "java", ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(2))));

            assertTrue(integerResult1.isSuccess());
            assertTrue(stringResult.isSuccess());
            assertTrue(integerResult2.isSuccess());

            assertEquals(Boolean.TRUE, integerResult1.getValue());
            assertEquals(Boolean.TRUE, stringResult.getValue());
            assertEquals(Boolean.TRUE, integerResult2.getValue());

            assertFalse((Boolean) integerResult1.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
            assertFalse((Boolean) stringResult.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
            assertTrue((Boolean) integerResult2.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
        } finally {
            executor.close();
        }
    }
}
