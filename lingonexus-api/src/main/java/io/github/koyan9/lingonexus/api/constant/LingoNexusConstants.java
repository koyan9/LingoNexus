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

package io.github.koyan9.lingonexus.api.constant;

/**
 * Shared constants used by LingoNexus.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public final class LingoNexusConstants {

    private LingoNexusConstants() {
        // prevent instantiation
    }

    // Cache defaults
    public static final int DEFAULT_CACHE_MAX_SIZE = 1000;
    public static final long DEFAULT_CACHE_EXPIRE_SECONDS = 3600;

    // Executor defaults
    public static final long DEFAULT_EXECUTION_TIMEOUT_MS = 30000;
    public static final int DEFAULT_THREAD_POOL_CORE_SIZE = 4;
    public static final int DEFAULT_THREAD_POOL_MAX_SIZE = 16;
    public static final int DEFAULT_THREAD_POOL_QUEUE_CAPACITY = 100;

    // Built-in module names
    public static final String MODULE_MATH = "math";
    public static final String MODULE_STRING = "str";
    public static final String MODULE_DATE = "date";
    public static final String MODULE_JSON = "json";
    public static final String MODULE_COLLECTION = "col";
    public static final String MODULE_CONVERT = "convert";
    public static final String MODULE_VALIDATOR = "validator";
    public static final String MODULE_FORMATTER = "formatter";
    public static final String MODULE_CODEC = "codec";

    // Common error messages
    public static final String ERROR_SCRIPT_EXECUTION_FAILED = "Script execution failed";
    public static final String ERROR_UNSUPPORTED_LANGUAGE = "Unsupported script language: ";
    public static final String ERROR_SCRIPT_IS_NULL = "Script cannot be null or empty";
    public static final String ERROR_CONTEXT_IS_NULL = "Script context cannot be null";
    public static final String ERROR_SANDBOX_NOT_FOUND = "No sandbox found for language: ";
    public static final String ERROR_DIVISION_BY_ZERO = "Division by zero";

    // Spring configuration prefixes
    public static final String CONFIG_PREFIX = "lingonexus";
    public static final String CONFIG_CACHE_PREFIX = CONFIG_PREFIX + ".cache";
    public static final String CONFIG_SANDBOX_PREFIX = CONFIG_PREFIX + ".sandbox";

    // Date/time formats
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // Numeric defaults
    public static final int DEFAULT_DECIMAL_SCALE = 10;
    public static final int CURRENCY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;

    /**
     * Keys for user-provided metadata in {@code ScriptContext}.
     */
    public static final class MetadataKeys {

        private MetadataKeys() {
            // prevent instantiation
        }

        // Request identity
        public static final String REQUEST_ID = "requestId";
        public static final String SESSION_ID = "sessionId";
        public static final String USER_ID = "userId";
        public static final String TENANT_ID = "tenantId";
        public static final String TIMESTAMP = "timestamp";

        // Execution control
        public static final String PRIORITY = "priority";
        public static final String TIMEOUT_OVERRIDE = "timeoutOverride";
        public static final String DEBUG_MODE = "debugMode";
        public static final String LOG_LEVEL = "logLevel";
        public static final String DETAILED_RESULT_METADATA_ENABLED = "detailedResultMetadataEnabled";
        public static final String RESULT_METADATA_PROFILE = "resultMetadataProfile";
        public static final String RESULT_METADATA_CATEGORIES = "resultMetadataCategories";
        public static final String RESULT_METADATA_POLICY = "resultMetadataPolicy";

        // Business context
        public static final String BUSINESS_SCENE = "businessScene";
        public static final String ENVIRONMENT = "environment";
        public static final String VERSION = "version";
        public static final String REGION = "region";

        // Observability
        public static final String MONITORING_ENABLED = "monitoringEnabled";
        public static final String PERFORMANCE_THRESHOLD = "performanceThreshold";
        public static final String CORRELATION_ID = "correlationId";
        public static final String SOURCE = "source";
    }

    /**
     * Keys for execution metadata in {@code ScriptResult}.
     */
    public static final class ResultMetadataKeys {

        private ResultMetadataKeys() {
            // prevent instantiation
        }

        // Timing
        public static final String COMPILE_TIME = "compileTime";
        public static final String EXECUTION_TIME = "executionTime";
        public static final String QUEUE_WAIT_TIME = "queueWaitTime";
        public static final String SECURITY_VALIDATION_TIME = "securityValidationTime";
        public static final String CACHE_HIT = "cacheHit";
        public static final String CACHE_WAIT_TIME = "cacheWaitTime";
        public static final String TOTAL_TIME = "totalTime";
        public static final String WALL_TIME = "wallTime";

        // Environment
        public static final String SCRIPT_ENGINE = "scriptEngine";
        public static final String ISOLATION_MODE = "isolationMode";
        public static final String MODULES_USED = "modulesUsed";
        public static final String SECURITY_CHECKS = "securityChecks";
        public static final String THREAD_ID = "threadId";
        public static final String THREAD_NAME = "threadName";

        // Runtime stats
        public static final String MEMORY_USED = "memoryUsed";
        public static final String GC_COUNT = "gcCount";
        public static final String CPU_TIME = "cpuTime";

        // Error details
        public static final String ERROR_TYPE = "errorType";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String ERROR_STACK = "errorStack";
        public static final String ERROR_STAGE = "errorStage";
        public static final String ERROR_COMPONENT = "errorComponent";
        public static final String ERROR_REASON = "errorReason";
        public static final String ERROR_PATH = "errorPath";
        public static final String ERROR_VALUE_TYPE = "errorValueType";
        public static final String ERROR_DETAIL_REASON = "errorDetailReason";
    }
}
