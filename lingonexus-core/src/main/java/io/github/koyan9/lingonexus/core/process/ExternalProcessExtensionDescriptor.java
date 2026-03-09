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

import java.io.Serializable;
import java.util.Map;

/**
 * Descriptor for reconstructing policies/modules in an external worker.
 */
public class ExternalProcessExtensionDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String className;
    private final Map<String, Object> descriptor;

    public ExternalProcessExtensionDescriptor(String className, Map<String, Object> descriptor) {
        this.className = className;
        this.descriptor = descriptor;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Object> getDescriptor() {
        return descriptor;
    }
}
