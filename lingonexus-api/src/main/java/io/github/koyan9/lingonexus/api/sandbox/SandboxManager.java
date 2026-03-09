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
package io.github.koyan9.lingonexus.api.sandbox;

import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;

import java.util.List;
import java.util.Optional;

/**
 * 沙箱管理器接口，管理多个脚本沙箱
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface SandboxManager {

    /**
     * 注册沙箱
     *
     * @param sandbox 脚本沙箱
     */
    void registerSandbox(ScriptSandbox sandbox);

    /**
     * 注销沙箱
     *
     * @param language 脚本语言
     */
    void unregisterSandbox(String language);

    /**
     * 根据语言获取沙箱
     *
     * @param language 脚本语言
     * @return 脚本沙箱
     */
    Optional<ScriptSandbox> getSandbox(String language);

    /**
     * 获取所有沙箱
     *
     * @return 沙箱列表
     */
    List<ScriptSandbox> getAllSandboxes();

    /**
     * 获取所有支持的语言
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
    boolean supports(String language);
}
