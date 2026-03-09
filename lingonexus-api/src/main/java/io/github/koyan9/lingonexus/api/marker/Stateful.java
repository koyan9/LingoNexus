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

package io.github.koyan9.lingonexus.api.marker;

/**
 * 有状态标记接口
 *
 * <p>实现此接口的类表明该对象是有状态的，不应在多个实例之间共享。</p>
 *
 * <p>有状态对象的特征：</p>
 * <ul>
 *   <li>包含可变的实例字段</li>
 *   <li>方法调用会改变对象的内部状态</li>
 *   <li>可能不是线程安全的</li>
 *   <li>每个使用者应该拥有独立的实例</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>包含缓存的脚本沙箱（如 GroovySandbox、GraalJSSandbox）</li>
 *   <li>包含可变配置的组件</li>
 *   <li>缓存管理器</li>
 *   <li>注册表</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-04
 */
public interface Stateful {
    // 标记接口，无需定义方法
}
