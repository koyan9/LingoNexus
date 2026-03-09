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
package io.github.koyan9.lingonexus.api.executor;

import io.github.koyan9.lingonexus.api.config.ExecutorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@DisplayName("ThreadPoolManager Tests")
class ThreadPoolManagerTest {

    private final ThreadPoolManager manager = ThreadPoolManager.createIndependentManager();

    @AfterEach
    void tearDown() {
        manager.shutdownAll();
    }

    @Test
    @DisplayName("Should recreate isolated executor after shutdownAll")
    void shouldRecreateIsolatedExecutorAfterShutdownAll() {
        ExecutorConfig firstConfig = ExecutorConfig.builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .queueCapacity(16)
                .threadNamePrefix("Async-A-")
                .build();

        IsolatedScriptExecutor firstExecutor = manager.getSharedIsolatedExecutor(firstConfig);
        assertEquals(2, manager.getExecutor(ThreadPoolManager.PoolType.ISOLATED_EXECUTOR).getCorePoolSize());

        manager.shutdownAll();

        ExecutorConfig secondConfig = ExecutorConfig.builder()
                .corePoolSize(3)
                .maxPoolSize(6)
                .queueCapacity(32)
                .threadNamePrefix("Async-B-")
                .build();

        IsolatedScriptExecutor secondExecutor = manager.getSharedIsolatedExecutor(secondConfig);

        assertNotSame(firstExecutor, secondExecutor);
        assertEquals(3, manager.getExecutor(ThreadPoolManager.PoolType.ISOLATED_EXECUTOR).getCorePoolSize());
    }
}
