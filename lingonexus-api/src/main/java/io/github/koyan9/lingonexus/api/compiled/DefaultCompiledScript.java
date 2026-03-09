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
import io.github.koyan9.lingonexus.api.exception.ScriptCompilationException;
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;

import java.util.Objects;

/**
 * 编译后的脚本默认实现
 *
 * <p>特性：</p>
 * <ul>
 *   <li>创建对象时自动触发编译（调用 compileFunction）</li>
 *   <li>存储编译结果，避免重复编译</li>
 *   <li>执行时使用已编译的结果</li>
 *   <li>避免匿名内部类，方便序列化</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 在 JavaSandbox.compile() 中
 * return new DefaultCompiledScript(
 *     ScriptLanguage.JAVA,
 *     script,
 *     context,
 *     (src, ctx) -> {
 *         ExpressionEvaluator evaluator = new ExpressionEvaluator();
 *         evaluator.setParameters(...);
 *         evaluator.cook(src);  // 真正的编译
 *         return evaluator;
 *     },
 *     (compiled, ctx) -> {
 *         ExpressionEvaluator evaluator = (ExpressionEvaluator) compiled;
 *         return evaluator.evaluate(ctx.getVariables().values().toArray());
 *     }
 * );
 * }</pre>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-27
 */
public class DefaultCompiledScript implements CompiledScript {

    private final ScriptLanguage language;
    private final String source;
    private final Object compiledResult;
    private final ExecuteFunction executeFunction;

    /**
     * 构造函数 - 创建时自动触发编译
     *
     * @param language 脚本语言枚举
     * @param source 脚本源码
     * @param context 编译上下文
     * @param compileFunction 编译函数（如 evaluator.cook()）
     * @param executeFunction 执行函数
     */
    public DefaultCompiledScript(ScriptLanguage language,
                                 String source,
                                 ScriptContext context,
                                 CompileFunction compileFunction,
                                 ExecuteFunction executeFunction) {
        this.language = Objects.requireNonNull(language, "language cannot be null");
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.executeFunction = Objects.requireNonNull(executeFunction, "executeFunction cannot be null");

        try {
            // 在构造时执行真正的编译
            this.compiledResult = compileFunction.compile(source, context);
        } catch (Exception e) {
            throw new ScriptCompilationException("Failed to compile script", e);
        }
    }

    @Override
    public Object execute(ScriptContext context) {
        try {
            return executeFunction.execute(compiledResult, context);
        } catch (ScriptRuntimeException e) {
            // 如果已经是 ScriptRuntimeException，直接抛出，保留原始错误消息
            throw e;
        } catch (Exception e) {
            throw new ScriptRuntimeException("Failed to execute script", e);
        }
    }

    @Override
    public String getLanguage() {
        return language.getId();
    }

    @Override
    public String getSource() {
        return source;
    }

    /**
     * 获取脚本语言枚举
     *
     * @return 脚本语言枚举
     */
    public ScriptLanguage getScriptLanguage() {
        return language;
    }

    /**
     * 获取编译后的结果对象
     *
     * @return 编译结果（如 Evaluator）
     */
    public Object getCompiledResult() {
        return compiledResult;
    }
}
