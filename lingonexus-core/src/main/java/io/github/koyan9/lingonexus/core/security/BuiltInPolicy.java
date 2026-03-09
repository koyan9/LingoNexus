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

import io.github.koyan9.lingonexus.api.security.SecurityPolicy;

/**
 * 内置安全策略
 *
 * <p>注意：黑白名单控制已下放到 Sandbox 层面（通过 SandboxConfig 配置），
 * 这里只保留脚本大小检查策略。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-06
 */
public enum BuiltInPolicy {

    SCRIPT_SIZE_POLICY("ScriptSizePolicy", "脚本大小限制", new ScriptSizePolicy()),
    ;
    private final String policyName;
    private final String description;
    private final SecurityPolicy securityPolicy;

    private BuiltInPolicy(String policyName, String description, SecurityPolicy securityPolicy) {
        this.policyName = policyName;
        this.description = description;
        this.securityPolicy = securityPolicy;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getDescription() {
        return description;
    }

    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }
}
