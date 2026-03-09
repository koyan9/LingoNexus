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
package io.github.koyan9.lingonexus.api.module.spi;

import java.util.Map;

/**
 * 脚本模块接口，提供可扩展的功能模块
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface ScriptModule {

    /**
     * 获取模块名称
     *
     * @return 模块名称
     */
    String getName();

    /**
     * 获取模块提供的函数
     * <p>
     * <b>重要说明：</b>
     * <ul>
     *   <li>Map 的 key 必须与模块的公共方法名一致</li>
     *   <li>脚本执行时直接调用模块对象的公共方法，不通过此 Map</li>
     *   <li>此 Map 主要用于：
     *     <ul>
     *       <li>元数据查询（通过 ModuleRegistry.getFunction()）</li>
     *       <li>文档生成</li>
     *       <li>函数列表展示</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <pre>{@code
     * public class MyModule implements ScriptModule {
     *     @Override
     *     public Map<String, Object> getFunctions() {
     *         Map<String, Object> map = new HashMap<>();
     *         // key "parse" 必须与方法名 parse() 一致
     *         map.put("parse", (Function<String, Object>) this::parse);
     *         return map;
     *     }
     *
     *     // 脚本中调用：myModule.parse(str)
     *     public Object parse(String str) {
     *         // ...
     *     }
     * }
     * }</pre>
     *
     * @return 函数名称到函数对象的映射（key 必须与方法名一致）
     */
    Map<String, Object> getFunctions();

    /**
     * 检查模块是否包含指定函数
     *
     * @param functionName 函数名称
     * @return 是否包含
     */
    boolean hasFunction(String functionName);

    /**
     * 获取模块描述
     *
     * @return 模块描述
     */
    default String getDescription() {
        return "Script module: " + getName();
    }
}
