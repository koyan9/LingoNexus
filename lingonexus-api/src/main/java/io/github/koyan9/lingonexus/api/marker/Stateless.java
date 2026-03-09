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
 * 无状态标记接口
 *
 * <p>实现此接口的类表明该对象是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * <p>无状态对象的特征：</p>
 * <ul>
 *   <li>不包含可变的实例字段</li>
 *   <li>所有方法都是纯函数或幂等的</li>
 *   <li>线程安全</li>
 *   <li>可以被多个线程并发访问而不需要同步</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>工具类模块（如 StringModule、ValidatorModule）</li>
 *   <li>无状态的脚本沙箱</li>
 *   <li>其他可以安全共享的组件</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-04
 */
public interface Stateless {
    // 标记接口，无需定义方法
}
