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
package io.github.koyan9.lingonexus.testcase.nospring.core.module;

import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Module Registry Tests")
class DefaultModuleRegistryTest {

    @Test
    @DisplayName("Should increment version on effective changes")
    void shouldIncrementVersionOnEffectiveChanges() {
        DefaultModuleRegistry registry = new DefaultModuleRegistry();
        long version1 = registry.getVersion();

        registry.registerModule(new TestModule("alpha"));
        long version2 = registry.getVersion();
        assertTrue(version2 > version1);

        registry.registerModule(new TestModule("beta"));
        long version3 = registry.getVersion();
        assertTrue(version3 > version2);

        registry.unregisterModule("alpha");
        long version4 = registry.getVersion();
        assertTrue(version4 > version3);
    }

    @Test
    @DisplayName("Should not increment version when unregistering missing module")
    void shouldNotIncrementVersionWhenUnregisteringMissingModule() {
        DefaultModuleRegistry registry = new DefaultModuleRegistry();
        registry.registerModule(new TestModule("alpha"));
        long version = registry.getVersion();

        registry.unregisterModule("missing");

        assertEquals(version, registry.getVersion());
    }

    private static class TestModule implements ScriptModule {
        private final String name;

        private TestModule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, Object> getFunctions() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasFunction(String functionName) {
            return false;
        }
    }
}
