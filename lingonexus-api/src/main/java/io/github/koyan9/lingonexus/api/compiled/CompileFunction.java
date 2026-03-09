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
 * 编译函数接口
 *
 * <p>用于执行真正的脚本编译操作，如 evaluator.cook() 或 evaluator.compile()</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-27
 */
@FunctionalInterface
public interface CompileFunction {
    /**
     * 执行编译操作
     *
     * @param script 脚本源码
     * @param context 编译上下文
     * @return 编译后的结果对象（如 Evaluator）
     * @throws Exception 编译异常
     */
    Object compile(String script, ScriptContext context) throws Exception;
}
