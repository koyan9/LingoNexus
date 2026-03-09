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
package io.github.koyan9.lingonexus.testcase.nospring.support;

import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorConsumer;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExternalProcessDescriptorBenchmarkModule implements ScriptModule,
        ExternalProcessDescriptorProvider, ExternalProcessDescriptorConsumer {

    private String name = "benchmarkModule";

    public ExternalProcessDescriptorBenchmarkModule() {
    }

    public ExternalProcessDescriptorBenchmarkModule(String name) {
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

    @Override
    public Map<String, Object> toExternalProcessDescriptor() {
        Map<String, Object> descriptor = new HashMap<String, Object>();
        descriptor.put("name", name);
        return descriptor;
    }

    @Override
    public void loadExternalProcessDescriptor(Map<String, Object> descriptor) {
        Object moduleName = descriptor.get("name");
        if (moduleName instanceof String) {
            this.name = (String) moduleName;
        }
    }
}
