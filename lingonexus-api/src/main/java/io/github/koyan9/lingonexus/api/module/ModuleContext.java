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
package io.github.koyan9.lingonexus.api.module;

import java.util.Map;

/**
 * 模块上下文接口，为脚本模块提供执行上下文
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface ModuleContext {

    /**
     * 获取模块变量
     *
     * @return 变量映射
     */
    Map<String, Object> getVariables();

    /**
     * 获取变量值
     *
     * @param name 变量名称
     * @return 变量值
     */
    Object getVariable(String name);

    /**
     * 设置变量值
     *
     * @param name 变量名称
     * @param value 变量值
     */
    void setVariable(String name, Object value);

    /**
     * 检查是否包含变量
     *
     * @param name 变量名称
     * @return 是否包含
     */
    boolean hasVariable(String name);

    /**
     * 移除变量
     *
     * @param name 变量名称
     */
    void removeVariable(String name);

    /**
     * 清空所有变量
     */
    void clear();
}
