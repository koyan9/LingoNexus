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

package io.github.koyan9.lingonexus.api.security;

import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;

/**
 * 安全策略接口（责任链模式）
 *
 * <p>用于在脚本执行前进行安全校验，支持多个策略串联。
 * 所有安全检查（脚本大小、黑白名单等）都在脚本执行前完成，
 * 如果任何一个策略校验失败，则直接返回失败结果。</p>
 *
 * <p>注意：脚本执行超时应该由 {@link SandboxConfig#getTimeoutMs()} 控制，
 * 不应该在 SecurityPolicy 中处理。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-05
 */
public interface SecurityPolicy {

    /**
     * 校验脚本是否符合安全策略
     *
     * <p>此方法在脚本执行前调用，用于检查脚本是否满足安全要求。
     * 如果校验失败，应该返回包含失败原因的 ValidationResult。</p>
     *
     * @param script 脚本内容
     * @param language 脚本语言
     * @param context 脚本上下文
     * @param config 沙箱配置
     * @return 校验结果，包含是否通过和失败原因
     */
    ValidationResult validate(String script, String language, ScriptContext context, SandboxConfig config);

    /**
     * 获取策略名称
     *
     * <p>用于日志记录和错误提示，应该返回一个简短且有意义的名称。</p>
     *
     * @return 策略名称
     */
    String getName();
}
