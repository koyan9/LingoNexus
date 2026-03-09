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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Result Metadata Policy Registry Loader Tests")
class ResultMetadataPolicyRegistryLoaderTest {

    @Test
    @DisplayName("Should load templates from properties")
    void shouldLoadTemplatesFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("resultMetadataPolicies.teamTiming.parent", "timing");
        properties.setProperty("resultMetadataPolicies.teamTiming.categories", "thread,module");

        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistryLoader.load(properties);
        Map<String, ResultMetadataPolicyTemplate> templates = registry.snapshotTemplates();

        assertEquals(1, templates.size());
        assertTrue(templates.containsKey("TEAMTIMING"));
        assertEquals("timing", templates.get("TEAMTIMING").getParentPolicyName());
        assertTrue(templates.get("TEAMTIMING").getCategories().contains(ResultMetadataCategory.THREAD));
        assertTrue(templates.get("TEAMTIMING").getCategories().contains(ResultMetadataCategory.MODULE));
    }

    @Test
    @DisplayName("Should load templates from map")
    void shouldLoadTemplatesFromMap() {
        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistryLoader.load(
                Collections.<String, Object>singletonMap("resultMetadataPolicies.debugLite.categories", "timing,error_diagnostics")
        );

        Map<String, ResultMetadataPolicyTemplate> templates = registry.snapshotTemplates();
        assertEquals(1, templates.size());
        assertTrue(templates.get("DEBUGLITE").getCategories().contains(ResultMetadataCategory.TIMING));
        assertTrue(templates.get("DEBUGLITE").getCategories().contains(ResultMetadataCategory.ERROR_DIAGNOSTICS));
    }

    @Test
    @DisplayName("Should load templates from input stream")
    void shouldLoadTemplatesFromInputStream() throws Exception {
        String payload = "resultMetadataPolicies.teamThread.parent=minimal\n"
                + "resultMetadataPolicies.teamThread.categories=thread\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));

        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistryLoader.load(inputStream);
        Map<String, ResultMetadataPolicyTemplate> templates = registry.snapshotTemplates();

        assertEquals(1, templates.size());
        assertEquals("minimal", templates.get("TEAMTHREAD").getParentPolicyName());
        assertTrue(templates.get("TEAMTHREAD").getCategories().contains(ResultMetadataCategory.THREAD));
    }
}
