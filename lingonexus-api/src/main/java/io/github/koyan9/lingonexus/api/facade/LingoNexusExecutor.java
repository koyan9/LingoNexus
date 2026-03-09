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
package io.github.koyan9.lingonexus.api.facade;

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ScriptException;
import io.github.koyan9.lingonexus.api.exception.ScriptSecurityException;
import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 脚本引擎接口，提供统一的脚本执行入口
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface LingoNexusExecutor extends LifecycleAware {

    /**
     * 执行脚本
     *
     * @param script 脚本内容，不能为 null
     * @param language 脚本语言，支持 groovy/javascript
     * @param context 脚本上下文，包含变量和参数
     * @return 执行结果，包含状态和数据
     * @throws ScriptException 脚本执行异常
     * @throws ScriptSecurityException 脚本安全异常
     */
    ScriptResult execute(String script, String language, ScriptContext context);

    /**
     * 异步执行脚本
     *
     * @param script 脚本内容
     * @param language 脚本语言
     * @param context 脚本上下文
     * @return 异步执行结果
     */
    CompletableFuture<ScriptResult> executeAsync(String script, String language, ScriptContext context);

    /**
     * 批量执行脚本
     *
     * @param scripts 脚本列表
     * @param language 脚本语言
     * @param context 脚本上下文
     * @return 执行结果列表
     */
    List<ScriptResult> executeBatch(List<String> scripts, String language, ScriptContext context);

    /**
     * 获取支持的脚本语言列表
     *
     * @return 语言标识列表
     */
    List<String> getSupportedLanguages();

    /**
     * 检查是否支持指定语言
     *
     * @param language 脚本语言
     * @return 是否支持
     */
    boolean isSupported(String language);

    /**
     * 手动注册 module
     *
     * @param module 脚本模块
     */
    void registerModule(ScriptModule module);

    /**
     * 注销模块
     *
     * @param moduleName 模块名称
     */
    void unregisterModule(String moduleName);

    /**
     * Returns aggregated execution statistics.
     *
     * @return execution statistics snapshot
     */
    EngineStatistics getStatistics();

    /**
     * Returns runtime diagnostics including cache and worker-pool state.
     *
     * @return diagnostics snapshot
     */
    EngineDiagnostics getDiagnostics();

}
