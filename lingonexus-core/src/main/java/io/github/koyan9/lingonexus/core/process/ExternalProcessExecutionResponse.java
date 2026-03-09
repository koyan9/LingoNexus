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
package io.github.koyan9.lingonexus.core.process;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Serializable response payload for one-shot external process execution.
 */
public class ExternalProcessExecutionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String status;
    private final Object value;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    private final long executionTime;
    private final Map<String, Long> executorCacheStatistics;
    private final String protocolVersion;
    private final List<String> supportedTransportProtocolCapabilities;
    private final List<String> supportedTransportSerializerContractIds;

    public ExternalProcessExecutionResponse(boolean success, String status, Object value,
                                            String errorMessage, Map<String, Object> metadata,
                                            long executionTime) {
        this(success, status, value, errorMessage, metadata, executionTime, Collections.<String, Long>emptyMap(), null, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    public ExternalProcessExecutionResponse(boolean success, String status, Object value,
                                            String errorMessage, Map<String, Object> metadata,
                                            long executionTime, Map<String, Long> executorCacheStatistics) {
        this(success, status, value, errorMessage, metadata, executionTime, executorCacheStatistics,
                null, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    public ExternalProcessExecutionResponse(boolean success, String status, Object value,
                                            String errorMessage, Map<String, Object> metadata,
                                            long executionTime, Map<String, Long> executorCacheStatistics,
                                            String protocolVersion,
                                            List<String> supportedTransportProtocolCapabilities,
                                            List<String> supportedTransportSerializerContractIds) {
        this.success = success;
        this.status = status;
        this.value = value;
        this.errorMessage = errorMessage;
        this.metadata = metadata;
        this.executionTime = executionTime;
        this.executorCacheStatistics = executorCacheStatistics;
        this.protocolVersion = protocolVersion;
        this.supportedTransportProtocolCapabilities = supportedTransportProtocolCapabilities != null
                ? supportedTransportProtocolCapabilities
                : Collections.<String>emptyList();
        this.supportedTransportSerializerContractIds = supportedTransportSerializerContractIds != null
                ? supportedTransportSerializerContractIds
                : Collections.<String>emptyList();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public Object getValue() {
        return value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public Map<String, Long> getExecutorCacheStatistics() {
        return executorCacheStatistics;
    }


    public String getProtocolVersion() {
        return protocolVersion;
    }

    public List<String> getSupportedTransportProtocolCapabilities() {
        return supportedTransportProtocolCapabilities;
    }

    public List<String> getSupportedTransportSerializerContractIds() {
        return supportedTransportSerializerContractIds;
    }
}
