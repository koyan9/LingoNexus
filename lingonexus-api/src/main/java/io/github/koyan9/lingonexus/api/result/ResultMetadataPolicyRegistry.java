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
 * Shared application-level registry for reusable result metadata policy templates.
 */
public class ResultMetadataPolicyRegistry {

    private final Map<String, ResultMetadataPolicyTemplate> templates = new LinkedHashMap<String, ResultMetadataPolicyTemplate>();

    public ResultMetadataPolicyRegistry registerTemplate(ResultMetadataPolicyTemplate template) {
        if (template != null) {
            templates.put(template.getName(), template);
        }
        return this;
    }

    public ResultMetadataPolicyRegistry registerTemplates(Map<String, ResultMetadataPolicyTemplate> templates) {
        if (templates != null) {
            for (ResultMetadataPolicyTemplate template : templates.values()) {
                registerTemplate(template);
            }
        }
        return this;
    }

    public ResultMetadataPolicyRegistry registerProperties(Properties properties) {
        return registerTemplates(ResultMetadataPolicyRegistryLoader.load(properties).snapshotTemplates());
    }

    public ResultMetadataPolicyRegistry registerPropertyMap(Map<String, ?> properties) {
        return registerTemplates(ResultMetadataPolicyRegistryLoader.load(properties).snapshotTemplates());
    }

    public ResultMetadataPolicyRegistry registerInputStream(InputStream inputStream) throws IOException {
        return registerTemplates(ResultMetadataPolicyRegistryLoader.load(inputStream).snapshotTemplates());
    }

    public Map<String, ResultMetadataPolicyTemplate> snapshotTemplates() {
        return ResultMetadataPolicySupport.normalizeTemplates(templates);
    }

    public static ResultMetadataPolicyRegistry create() {
        return new ResultMetadataPolicyRegistry();
    }
}
