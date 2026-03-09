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
 * 执行函数接口
 *
 * <p>用于执行已编译的脚本</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-27
 */
@FunctionalInterface
public interface ExecuteFunction {
    /**
     * 执行已编译的脚本
     *
     * @param compiled 编译后的对象（如 Evaluator）
     * @param context 执行上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    Object execute(Object compiled, ScriptContext context) throws Exception;
}
