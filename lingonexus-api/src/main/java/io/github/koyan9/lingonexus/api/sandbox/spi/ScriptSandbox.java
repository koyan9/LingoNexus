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
package io.github.koyan9.lingonexus.api.sandbox.spi;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;

import java.util.List;

/**
 * 脚本沙箱接口，提供安全的脚本执行环境
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface ScriptSandbox extends LifecycleAware {

    /**
     * 编译脚本（带上下文，支持变量类型信息的编译优化）
     *
     * @param script 脚本内容
     * @param context 脚本上下文（包含变量类型信息）
     * @return 编译后的脚本
     */
    CompiledScript compile(String script, ScriptContext context);

    /**
     * 获取支持的脚本语言列表
     *
     * @return 语言标识列表
     */
    List<String> getSupportedLanguages();

    /**
     * 获取沙箱配置
     *
     * @return 沙箱配置
     */
    SandboxConfig getConfig();

    /**
     * 检查是否支持指定语言
     *
     * @param language 脚本语言
     * @return 是否支持
     */
    boolean supports(String language);
}
