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

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * LingoNexusContext 测试类
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-03
 */
@DisplayName("LingoNexusContext Tests")
class LingoNexusContextTest {

    private LingoNexusConfig mockConfig;
    private LingoNexusExecutor mockExecutor;

    @BeforeEach
    void setUp() {
        // 清除全局上下文
        LingoNexusContext.clear();

        // 创建 mock 对象
        mockConfig = mock(LingoNexusConfig.class);
        mockExecutor = mock(LingoNexusExecutor.class);
    }

    @AfterEach
    void tearDown() {
        // 清除全局上下文
        LingoNexusContext.clear();
    }

    @Test
    @DisplayName("Should set and get context successfully")
    void shouldSetAndGetContext() {
        // When
        LingoNexusContext.setContext(mockConfig, mockExecutor);

        // Then
        assertTrue(LingoNexusContext.isInitialized());
        assertSame(mockConfig, LingoNexusContext.getConfig());
        assertSame(mockExecutor, LingoNexusContext.getExecutor());
    }

    @Test
    @DisplayName("Should throw exception when getting config before initialization")
    void shouldThrowExceptionWhenGettingConfigBeforeInit() {
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            LingoNexusContext::getConfig
        );
        assertTrue(exception.getMessage().contains("not been initialized"));
    }

    @Test
    @DisplayName("Should throw exception when getting executor before initialization")
    void shouldThrowExceptionWhenGettingExecutorBeforeInit() {
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            LingoNexusContext::getExecutor
        );
        assertTrue(exception.getMessage().contains("not been initialized"));
    }

    @Test
    @DisplayName("Should throw exception when setting null config")
    void shouldThrowExceptionWhenSettingNullConfig() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> LingoNexusContext.setContext(null, mockExecutor)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should throw exception when setting null executor")
    void shouldThrowExceptionWhenSettingNullExecutor() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> LingoNexusContext.setContext(mockConfig, null)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("Should clear context successfully")
    void shouldClearContext() {
        // Given
        LingoNexusContext.setContext(mockConfig, mockExecutor);
        assertTrue(LingoNexusContext.isInitialized());

        // When
        LingoNexusContext.clear();

        // Then
        assertFalse(LingoNexusContext.isInitialized());
        assertThrows(IllegalStateException.class, LingoNexusContext::getConfig);
        assertThrows(IllegalStateException.class, LingoNexusContext::getExecutor);
    }

    @Test
    @DisplayName("Should return false when not initialized")
    void shouldReturnFalseWhenNotInitialized() {
        // When & Then
        assertFalse(LingoNexusContext.isInitialized());
    }

    @Test
    @DisplayName("Should update context when set multiple times")
    void shouldUpdateContextWhenSetMultipleTimes() {
        // Given
        LingoNexusConfig newConfig = mock(LingoNexusConfig.class);
        LingoNexusExecutor newExecutor = mock(LingoNexusExecutor.class);

        // When
        LingoNexusContext.setContext(mockConfig, mockExecutor);
        LingoNexusContext.setContext(newConfig, newExecutor);

        // Then
        assertSame(newConfig, LingoNexusContext.getConfig());
        assertSame(newExecutor, LingoNexusContext.getExecutor());
    }
}
