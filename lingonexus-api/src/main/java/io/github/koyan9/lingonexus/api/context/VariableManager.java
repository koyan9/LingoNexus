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

package io.github.koyan9.lingonexus.api.context;

import java.util.Map;

/**
 * 变量管理器接口
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-03
 */
public interface VariableManager {

    /**
     * 设置变量
     *
     * @param name 变量名
     * @param value 变量值
     */
    void setVariable(String name, Object value);

    /**
     * 获取变量
     *
     * @param name 变量名
     * @return 变量值
     */
    Object getVariable(String name);

    /**
     * 检查变量是否存在
     *
     * @param name 变量名
     * @return 是否存在
     */
    boolean hasVariable(String name);

    /**
     * 移除变量
     *
     * @param name 变量名
     */
    void removeVariable(String name);

    /**
     * 清空所有变量
     */
    void clear();

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    long getVersion();

    /**
     * 获取所有变量
     *
     * @return 所有变量的副本
     */
    Map<String, Object> getAllVariables();
}
