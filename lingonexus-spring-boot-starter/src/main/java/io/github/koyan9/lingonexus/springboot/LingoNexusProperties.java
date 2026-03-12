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

package io.github.koyan9.lingonexus.springboot;

import io.github.koyan9.lingonexus.api.config.ExecutorConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicy;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * LingoNexus Spring Boot 配置属性
 *
 * <p>配置示例 (application.yml):</p>
 * <pre>{@code
 * lingonexus:
 *   enabled: true
 *   default-language: groovy
 *   cache:
 *     enabled: true
 *     max-size: 1000
 *     expire-after-write-ms: 3600000
 *     expire-after-access-ms: 1800000
 *   sandbox:
 *     enabled: true
 *     max-script-size: 65536
 *     timeout-ms: 5000
 *     enable-engine-cache: true
 *     class-whitelist:
 *       - "java.lang.*"
 *       - "java.util.*"
 *     class-blacklist:
 *       - "java.io.*"
 *       - "java.nio.*"
 *       - "java.lang.reflect.*"
 *   executor:
 *     core-pool-size: 8
 *     max-pool-size: 16
 *     keep-alive-time-seconds: 60
 *     queue-capacity: 1000
 *     thread-name-prefix: ScriptExecutor-Async-
 *     rejection-policy: CALLER_RUNS
 *   exclude-script-modules:
 *     - "io.github.koyan9.lingonexus.utils.MathModule"  # 类全限定名
 *     - "json"  # module name
 *   allowed-script-modules:
 *     - "io.github.koyan9.lingonexus.utils.*"  # 支持通配符
 *     - "math"  # module name
 *   global-variables:
 *     appName: "MyApp"
 *     version: "1.0.0"
 *     maxRetries: 3
 * }</pre>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
@ConfigurationProperties(prefix = LingoNexusConstants.CONFIG_PREFIX)
public class LingoNexusProperties {
    private boolean enabled = true;
    private String defaultLanguage = ScriptLanguage.getDefault().getId();
    private CacheProperties cache = new CacheProperties();
    private SandboxProperties sandbox = new SandboxProperties();
    private ExecutorProperties executor = new ExecutorProperties();
    private IsolatedExecutorProperties isolatedExecutor = new IsolatedExecutorProperties();
    private MetadataProperties metadata = new MetadataProperties();
    private Set<String> excludeScriptModules;
    private Set<String> allowedScriptModules;
    private Map<String, Object> globalVariables = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public SandboxProperties getSandbox() {
        return sandbox;
    }

    public void setSandbox(SandboxProperties sandbox) {
        this.sandbox = sandbox;
    }

    public ExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorProperties executor) {
        this.executor = executor;
    }

    public IsolatedExecutorProperties getIsolatedExecutor() {
        return isolatedExecutor;
    }

    public void setIsolatedExecutor(IsolatedExecutorProperties isolatedExecutor) {
        this.isolatedExecutor = isolatedExecutor;
    }

    public MetadataProperties getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataProperties metadata) {
        this.metadata = metadata;
    }

    public Set<String> getExcludeScriptModules() {
        return excludeScriptModules;
    }

    public void setExcludeScriptModules(Set<String> excludeScriptModules) {
        this.excludeScriptModules = excludeScriptModules;
    }

    public Set<String> getAllowedScriptModules() {
        return allowedScriptModules;
    }

    public void setAllowedScriptModules(Set<String> allowedScriptModules) {
        this.allowedScriptModules = allowedScriptModules;
    }

    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(Map<String, Object> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public static class CacheProperties {
        private boolean enabled = true;
        private int maxSize = 1000;
        private long expireAfterWriteMs = 3600000;
        private long expireAfterAccessMs = 1800000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public long getExpireAfterWriteMs() {
            return expireAfterWriteMs;
        }

        public void setExpireAfterWriteMs(long expireAfterWriteMs) {
            this.expireAfterWriteMs = expireAfterWriteMs;
        }

        public long getExpireAfterAccessMs() {
            return expireAfterAccessMs;
        }

        public void setExpireAfterAccessMs(long expireAfterAccessMs) {
            this.expireAfterAccessMs = expireAfterAccessMs;
        }
    }

    public static class SandboxProperties {
        private boolean enabled = true;
        private int maxScriptSize = 65536;
        private long timeoutMs = 5000;
        private boolean enableEngineCache = true;
        private ExecutionIsolationMode isolationMode = ExecutionIsolationMode.AUTO;
        private int externalProcessPoolSize = 1;
        private int externalProcessStartupRetries = 2;
        private int externalProcessPrewarmCount = 1;
        private long externalProcessIdleTtlMs = 300000L;
        private long externalProcessBorrowTimeoutMs = -1L;
        private int externalProcessExecutorCacheMaxSize = 8;
        private long externalProcessExecutorCacheIdleTtlMs = 300000L;
        private Set<String> classWhitelist = new HashSet<>();
        private Set<String> classBlacklist = new HashSet<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxScriptSize() {
            return maxScriptSize;
        }

        public void setMaxScriptSize(int maxScriptSize) {
            this.maxScriptSize = maxScriptSize;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public boolean isEnableEngineCache() {
            return enableEngineCache;
        }

        public void setEnableEngineCache(boolean enableEngineCache) {
            this.enableEngineCache = enableEngineCache;
        }

        public ExecutionIsolationMode getIsolationMode() {
            return isolationMode;
        }

        public void setIsolationMode(ExecutionIsolationMode isolationMode) {
            this.isolationMode = isolationMode;
        }

        public int getExternalProcessPoolSize() {
            return externalProcessPoolSize;
        }

        public void setExternalProcessPoolSize(int externalProcessPoolSize) {
            this.externalProcessPoolSize = externalProcessPoolSize;
        }

        public int getExternalProcessStartupRetries() {
            return externalProcessStartupRetries;
        }

        public void setExternalProcessStartupRetries(int externalProcessStartupRetries) {
            this.externalProcessStartupRetries = externalProcessStartupRetries;
        }

        public int getExternalProcessPrewarmCount() {
            return externalProcessPrewarmCount;
        }

        public void setExternalProcessPrewarmCount(int externalProcessPrewarmCount) {
            this.externalProcessPrewarmCount = externalProcessPrewarmCount;
        }

        public long getExternalProcessIdleTtlMs() {
            return externalProcessIdleTtlMs;
        }

        public void setExternalProcessIdleTtlMs(long externalProcessIdleTtlMs) {
            this.externalProcessIdleTtlMs = externalProcessIdleTtlMs;
        }

        public long getExternalProcessBorrowTimeoutMs() {
            return externalProcessBorrowTimeoutMs;
        }

        public void setExternalProcessBorrowTimeoutMs(long externalProcessBorrowTimeoutMs) {
            this.externalProcessBorrowTimeoutMs = externalProcessBorrowTimeoutMs;
        }

        public int getExternalProcessExecutorCacheMaxSize() {
            return externalProcessExecutorCacheMaxSize;
        }

        public void setExternalProcessExecutorCacheMaxSize(int externalProcessExecutorCacheMaxSize) {
            this.externalProcessExecutorCacheMaxSize = externalProcessExecutorCacheMaxSize;
        }

        public long getExternalProcessExecutorCacheIdleTtlMs() {
            return externalProcessExecutorCacheIdleTtlMs;
        }

        public void setExternalProcessExecutorCacheIdleTtlMs(long externalProcessExecutorCacheIdleTtlMs) {
            this.externalProcessExecutorCacheIdleTtlMs = externalProcessExecutorCacheIdleTtlMs;
        }

        public Set<String> getClassWhitelist() {
            return classWhitelist;
        }

        public void setClassWhitelist(Set<String> classWhitelist) {
            this.classWhitelist = classWhitelist;
        }

        public Set<String> getClassBlacklist() {
            return classBlacklist;
        }

        public void setClassBlacklist(Set<String> classBlacklist) {
            this.classBlacklist = classBlacklist;
        }
    }

    public static class ExecutorProperties {
        private int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private int maxPoolSize = Math.max(Runtime.getRuntime().availableProcessors() * 4, 50);
        private long keepAliveTimeSeconds = 60L;
        private int queueCapacity = 1000;
        private String threadNamePrefix = "ScriptExecutor-Async-";
        private ExecutorConfig.RejectionPolicy rejectionPolicy = ExecutorConfig.RejectionPolicy.CALLER_RUNS;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public long getKeepAliveTimeSeconds() {
            return keepAliveTimeSeconds;
        }

        public void setKeepAliveTimeSeconds(long keepAliveTimeSeconds) {
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public ExecutorConfig.RejectionPolicy getRejectionPolicy() {
            return rejectionPolicy;
        }

        public void setRejectionPolicy(ExecutorConfig.RejectionPolicy rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
        }
    }

    public static class IsolatedExecutorProperties extends ExecutorProperties {

        public IsolatedExecutorProperties() {
            setCorePoolSize(32);
            setMaxPoolSize(256);
            setQueueCapacity(2000);
            setThreadNamePrefix("ScriptExecutor-Isolated-");
        }
    }

    public static class MetadataProperties {
        private ResultMetadataProfile profile;
        private ResultMetadataPolicy policy;
        private String policyName;
        private Set<ResultMetadataCategory> categories = new HashSet<ResultMetadataCategory>();
        private Map<String, PolicyTemplateProperties> policyTemplates = new LinkedHashMap<String, PolicyTemplateProperties>();

        public ResultMetadataProfile getProfile() {
            return profile;
        }

        public void setProfile(ResultMetadataProfile profile) {
            this.profile = profile;
        }

        public ResultMetadataPolicy getPolicy() {
            return policy;
        }

        public void setPolicy(ResultMetadataPolicy policy) {
            this.policy = policy;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public Set<ResultMetadataCategory> getCategories() {
            return categories;
        }

        public void setCategories(Set<ResultMetadataCategory> categories) {
            this.categories = categories;
        }

        public Map<String, PolicyTemplateProperties> getPolicyTemplates() {
            return policyTemplates;
        }

        public void setPolicyTemplates(Map<String, PolicyTemplateProperties> policyTemplates) {
            this.policyTemplates = policyTemplates;
        }
    }

    public static class PolicyTemplateProperties {
        private String parentPolicyName;
        private Set<ResultMetadataCategory> categories = new HashSet<ResultMetadataCategory>();

        public String getParentPolicyName() {
            return parentPolicyName;
        }

        public void setParentPolicyName(String parentPolicyName) {
            this.parentPolicyName = parentPolicyName;
        }

        public Set<ResultMetadataCategory> getCategories() {
            return categories;
        }

        public void setCategories(Set<ResultMetadataCategory> categories) {
            this.categories = categories;
        }
    }
}
