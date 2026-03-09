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
package io.github.koyan9.lingonexus.api.compiled;

import io.github.koyan9.lingonexus.api.context.ScriptContext;

/**
 * 编译后的脚本接口
 *
 * @author OpenScriptEngine Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface CompiledScript {
    /**
     * 执行编译后的脚本
     *
     * @param context 脚本上下文
     * @return 执行结果
     */
    Object execute(ScriptContext context);

    /**
     * 获取脚本语言
     *
     * @return 脚本语言标识
     */
    String getLanguage();

    /**
     * 获取脚本源码
     *
     * @return 脚本源码
     */
    String getSource();

}
