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
package io.github.koyan9.lingonexus.api.executor;

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("ScriptTask Feature Test")
class ScriptTaskFeatureTest {

    @Test
    @DisplayName("Should create script task via static factory")
    void shouldCreateScriptTaskViaStaticFactory() {
        ScriptContext context = ScriptContext.of(Collections.<String, Object>emptyMap());

        ScriptTask task = ScriptTask.of("1", "return 1 + 1", "groovy", context);

        assertEquals("1", task.getId());
        assertEquals("return 1 + 1", task.getScript());
        assertEquals("groovy", task.getLanguage());
        assertSame(context, task.getContext());
    }
}
