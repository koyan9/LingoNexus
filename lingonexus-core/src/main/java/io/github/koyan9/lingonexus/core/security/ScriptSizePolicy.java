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

package io.github.koyan9.lingonexus.core.security;

import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;

import java.nio.charset.StandardCharsets;

/**
 * 脚本大小限制策略
 *
 * <p>检查脚本大小是否超过配置的最大值</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-05
 */
public class ScriptSizePolicy implements SecurityPolicy {

    @Override
    public ValidationResult validate(String script, String language, ScriptContext context, SandboxConfig config) {
        if (script == null) {
            return ValidationResult.failure("Script cannot be null");
        }

        int scriptSize = script.getBytes(StandardCharsets.UTF_8).length;
        int maxScriptSize = config.getMaxScriptSize();

        if (scriptSize <= 0) {
            return ValidationResult.failure("Script size must be greater than 0");
        }

        if (scriptSize > maxScriptSize) {
            return ValidationResult.failure(
                String.format("Script size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    scriptSize, maxScriptSize)
            );
        }

        return ValidationResult.success();
    }

    @Override
    public String getName() {
        return "ScriptSizePolicy";
    }
}
