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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON length-prefixed protocol codec for external process workers.
 */
public final class ExternalProcessProtocolCodec {

    private static final String PROTOCOL_VERSION = "1";
    private static final int REQUEST_PAYLOAD_CAPACITY = 48;
    private static final int RESPONSE_PAYLOAD_CAPACITY = 16;
    private static final int DESCRIPTOR_PAYLOAD_CAPACITY = 4;
    private static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;
    private static final java.util.List<String> SUPPORTED_TRANSPORT_PROTOCOL_CAPABILITIES = java.util.Collections.unmodifiableList(
            java.util.Arrays.asList(
                    io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability.JSON_FRAMED.name(),
                    io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability.CUSTOM_SERIALIZER_CONTRACT.name()
            )
    );
    private static final java.util.List<String> SUPPORTED_TRANSPORT_SERIALIZER_CONTRACT_IDS = java.util.Collections.emptyList();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() { };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ExternalProcessProtocolCodec() {
    }

    public static void writeRequest(OutputStream outputStream, ExternalProcessExecutionRequest request) throws IOException {
        Map<String, Object> payload = new HashMap<String, Object>(REQUEST_PAYLOAD_CAPACITY);
        payload.put("protocolVersion", PROTOCOL_VERSION);
        payload.put("requestType", request.getRequestType().name());
        payload.put("script", request.getScript());
        payload.put("language", request.getLanguage());
        payload.put("defaultLanguage", request.getDefaultLanguage());
        payload.put("cacheEnabled", request.isCacheEnabled());
        payload.put("cacheMaxSize", request.getCacheMaxSize());
        payload.put("cacheExpireAfterWriteMs", request.getCacheExpireAfterWriteMs());
        payload.put("cacheExpireAfterAccessMs", request.getCacheExpireAfterAccessMs());
        payload.put("externalProcessExecutorCacheMaxSize", request.getExternalProcessExecutorCacheMaxSize());
        payload.put("externalProcessExecutorCacheIdleTtlMs", request.getExternalProcessExecutorCacheIdleTtlMs());
        payload.put("sandboxEnabled", request.isSandboxEnabled());
        payload.put("maxScriptSize", request.getMaxScriptSize());
        payload.put("enableEngineCache", request.isEnableEngineCache());
        payload.put("classWhitelist", request.getClassWhitelist() != null ? request.getClassWhitelist() : Collections.emptySet());
        payload.put("classBlacklist", request.getClassBlacklist() != null ? request.getClassBlacklist() : Collections.emptySet());
        payload.put("excludeScriptModules", request.getExcludeScriptModules() != null ? request.getExcludeScriptModules() : Collections.emptySet());
        payload.put("allowedScriptModules", request.getAllowedScriptModules() != null ? request.getAllowedScriptModules() : Collections.emptySet());
        payload.put("allowedSandboxImplementations", request.getAllowedSandboxImplementations() != null ? request.getAllowedSandboxImplementations() : Collections.emptySet());
        payload.put("allowedSandboxLanguages", request.getAllowedSandboxLanguages() != null ? request.getAllowedSandboxLanguages() : Collections.emptySet());
        payload.put("allowedSandboxHostAccessModes", request.getAllowedSandboxHostAccessModes() != null ? enumSetToNames(request.getAllowedSandboxHostAccessModes()) : Collections.emptyList());
        payload.put("allowedSandboxHostRestrictionModes", request.getAllowedSandboxHostRestrictionModes() != null ? enumSetToNames(request.getAllowedSandboxHostRestrictionModes()) : Collections.emptyList());
        payload.put("requiredSandboxHostRestrictionFlags", request.getRequiredSandboxHostRestrictionFlags() != null ? enumSetToNames(request.getRequiredSandboxHostRestrictionFlags()) : Collections.emptyList());
        payload.put("allowedSandboxResultTransportModes", request.getAllowedSandboxResultTransportModes() != null ? enumSetToNames(request.getAllowedSandboxResultTransportModes()) : Collections.emptyList());
        payload.put("allowedSandboxTransportSerializerModes", request.getAllowedSandboxTransportSerializerModes() != null ? enumSetToNames(request.getAllowedSandboxTransportSerializerModes()) : Collections.emptyList());
        payload.put("allowedSandboxTransportPayloadProfiles", request.getAllowedSandboxTransportPayloadProfiles() != null ? enumSetToNames(request.getAllowedSandboxTransportPayloadProfiles()) : Collections.emptyList());
        payload.put("requiredSandboxTransportProtocolCapabilities", request.getRequiredSandboxTransportProtocolCapabilities() != null ? enumSetToNames(request.getRequiredSandboxTransportProtocolCapabilities()) : Collections.emptyList());
        payload.put("requiredSandboxTransportSerializerContractIds", request.getRequiredSandboxTransportSerializerContractIds() != null ? request.getRequiredSandboxTransportSerializerContractIds() : Collections.emptySet());
        payload.put("requireEngineCacheCapableSandbox", request.isRequireEngineCacheCapableSandbox());
        payload.put("requireExternalProcessCompatibleSandbox", request.isRequireExternalProcessCompatibleSandbox());
        payload.put("requireJsonSafeExternalResult", request.isRequireJsonSafeExternalResult());
        payload.put("requireJsonSafeExternalMetadata", request.isRequireJsonSafeExternalMetadata());
        payload.put("resultMetadataProfile", request.getResultMetadataProfile().name());
        payload.put("resultMetadataCategories", request.getResultMetadataCategories() != null ? enumSetToNames(request.getResultMetadataCategories()) : Collections.emptyList());
        payload.put("customSecurityPolicies", normalizeDescriptors(request.getCustomSecurityPolicies()));
        payload.put("dynamicModules", normalizeDescriptors(request.getDynamicModules()));
        payload.put("variables", normalizeRequestPayload(request.getVariables()));
        payload.put("metadata", normalizeRequestPayload(request.getMetadata()));
        writeFrame(outputStream, payload);
    }

    public static ExternalProcessExecutionRequest readRequest(InputStream inputStream) throws IOException {
        Map<String, Object> payload = readFrame(inputStream);
        validateProtocolVersion(payload);
        String requestType = stringValue(payload.get("requestType"));
        return new ExternalProcessExecutionRequest(
                ExternalProcessExecutionRequest.RequestType.valueOf(requestType),
                stringValue(payload.get("script")),
                stringValue(payload.get("language")),
                stringValue(payload.get("defaultLanguage")),
                booleanValue(payload.get("cacheEnabled")),
                longValue(payload.get("cacheMaxSize")),
                longValue(payload.get("cacheExpireAfterWriteMs")),
                longValue(payload.get("cacheExpireAfterAccessMs")),
                intValue(payload.get("externalProcessExecutorCacheMaxSize")),
                longValue(payload.get("externalProcessExecutorCacheIdleTtlMs")),
                booleanValue(payload.get("sandboxEnabled")),
                intValue(payload.get("maxScriptSize")),
                booleanValue(payload.get("enableEngineCache")),
                setValue(payload.get("classWhitelist")),
                setValue(payload.get("classBlacklist")),
                setValue(payload.get("excludeScriptModules")),
                setValue(payload.get("allowedScriptModules")),
                setValue(payload.get("allowedSandboxImplementations")),
                setValue(payload.get("allowedSandboxLanguages")),
                hostAccessModeSetValue(payload.get("allowedSandboxHostAccessModes")),
                hostRestrictionModeSetValue(payload.get("allowedSandboxHostRestrictionModes")),
                hostRestrictionFlagSetValue(payload.get("requiredSandboxHostRestrictionFlags")),
                resultTransportModeSetValue(payload.get("allowedSandboxResultTransportModes")),
                transportSerializerModeSetValue(payload.get("allowedSandboxTransportSerializerModes")),
                transportPayloadProfileSetValue(payload.get("allowedSandboxTransportPayloadProfiles")),
                transportProtocolCapabilitySetValue(payload.get("requiredSandboxTransportProtocolCapabilities")),
                setValue(payload.get("requiredSandboxTransportSerializerContractIds")),
                booleanValue(payload.get("requireEngineCacheCapableSandbox")),
                booleanValue(payload.get("requireExternalProcessCompatibleSandbox")),
                booleanValue(payload.get("requireJsonSafeExternalResult")),
                booleanValue(payload.get("requireJsonSafeExternalMetadata")),
                ResultMetadataProfile.fromString(stringValue(payload.get("resultMetadataProfile"))),
                resultMetadataCategorySetValue(payload.get("resultMetadataCategories")),
                descriptorListValue(payload.get("customSecurityPolicies")),
                descriptorListValue(payload.get("dynamicModules")),
                mapValue(payload.get("variables")),
                mapValue(payload.get("metadata"))
        );
    }

    public static void writeResponse(OutputStream outputStream, ExternalProcessExecutionResponse response) throws IOException {
        Map<String, Object> payload = new HashMap<String, Object>(RESPONSE_PAYLOAD_CAPACITY);
        payload.put("protocolVersion", PROTOCOL_VERSION);
        payload.put("success", response.isSuccess());
        payload.put("status", response.getStatus());
        payload.put("value", JsonSafeValueNormalizer.normalizeForResponse(response.getValue()));
        payload.put("errorMessage", response.getErrorMessage());
        payload.put("metadata", normalizeResponseMetadata(response.getMetadata()));
        payload.put("executionTime", response.getExecutionTime());
        payload.put("executorCacheStatistics", response.getExecutorCacheStatistics() != null ? response.getExecutorCacheStatistics() : Collections.emptyMap());
        payload.put("supportedTransportProtocolCapabilities", response.getSupportedTransportProtocolCapabilities() != null ? response.getSupportedTransportProtocolCapabilities() : Collections.emptyList());
        payload.put("supportedTransportSerializerContractIds", response.getSupportedTransportSerializerContractIds() != null ? response.getSupportedTransportSerializerContractIds() : Collections.emptyList());
        writeFrame(outputStream, payload);
    }

    public static ExternalProcessExecutionResponse readResponse(InputStream inputStream) throws IOException {
        Map<String, Object> payload = readFrame(inputStream);
        validateProtocolVersion(payload);
        return new ExternalProcessExecutionResponse(
                booleanValue(payload.get("success")),
                stringValue(payload.get("status")),
                payload.get("value"),
                stringValue(payload.get("errorMessage")),
                mapValue(payload.get("metadata")),
                longValue(payload.get("executionTime")),
                longMapValue(payload.get("executorCacheStatistics")),
                stringValue(payload.get("protocolVersion")),
                stringListValue(payload.get("supportedTransportProtocolCapabilities")),
                stringListValue(payload.get("supportedTransportSerializerContractIds"))
        );
    }

    private static void writeFrame(OutputStream outputStream, Map<String, Object> payload) throws IOException {
        byte[] bytes = OBJECT_MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.flush();
    }

    private static Map<String, Object> readFrame(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int length = dataInputStream.readInt();
        if (length < 0) {
            throw new IOException("Negative frame length received from external worker");
        }
        if (length > MAX_FRAME_BYTES) {
            throw new IOException("External-process frame length exceeds limit: " + length);
        }
        byte[] bytes = new byte[length];
        dataInputStream.readFully(bytes);
        return OBJECT_MAPPER.readValue(bytes, MAP_TYPE);
    }

    private static void validateProtocolVersion(Map<String, Object> payload) throws IOException {
        String version = stringValue(payload.get("protocolVersion"));
        if (!PROTOCOL_VERSION.equals(version)) {
            throw new IOException("Unsupported external-process protocol version: " + version);
        }
    }

    private static Map<String, Object> normalizeRequestPayload(Map<String, Object> payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }
        if (JsonSafeValueNormalizer.isNormalizedMap(payload)) {
            return payload;
        }
        return JsonSafeValueNormalizer.normalizeMap(payload);
    }

    private static Map<String, Object> normalizeResponseMetadata(Map<String, Object> payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }
        if (JsonSafeValueNormalizer.isNormalizedMap(payload)) {
            return payload;
        }
        return JsonSafeValueNormalizer.normalizeMap(payload, "$.metadata", true);
    }


    public static String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }

    public static java.util.List<String> getSupportedTransportProtocolCapabilities() {
        return SUPPORTED_TRANSPORT_PROTOCOL_CAPABILITIES;
    }

    public static java.util.List<String> getSupportedTransportSerializerContractIds() {
        return SUPPORTED_TRANSPORT_SERIALIZER_CONTRACT_IDS;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    private static int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
    }

    private static long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<String> setValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<String>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) value;
    }


    @SuppressWarnings("unchecked")
    private static Map<String, Long> longMapValue(Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> raw = (Map<String, Object>) value;
        Map<String, Long> result = new HashMap<String, Long>(raw.size());
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), longValue(entry.getValue()));
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private static java.util.List<String> stringListValue(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.List<String> result = new java.util.ArrayList<String>(raw.size());
        for (Object item : raw) {
            result.add(String.valueOf(item));
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode> hostAccessModeSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode.valueOf(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode> hostRestrictionModeSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode.valueOf(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag> hostRestrictionFlagSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag.valueOf(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode> resultTransportModeSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode.valueOf(String.valueOf(item)));
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode> transportSerializerModeSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode.valueOf(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile> transportPayloadProfileSetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile.valueOf(String.valueOf(item)));
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability> transportProtocolCapabilitySetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability> result = new java.util.LinkedHashSet<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability.valueOf(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<ResultMetadataCategory> resultMetadataCategorySetValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        java.util.List<Object> raw = (java.util.List<Object>) value;
        java.util.LinkedHashSet<ResultMetadataCategory> result = new java.util.LinkedHashSet<ResultMetadataCategory>(resolveCollectionCapacity(raw.size()));
        for (Object item : raw) {
            result.add(ResultMetadataCategory.valueOf(String.valueOf(item)));
        }
        return result;
    }

    private static int resolveCollectionCapacity(int expectedSize) {
        if (expectedSize <= 0) {
            return 16;
        }
        return (int) ((expectedSize / 0.75f) + 1.0f);
    }

    private static java.util.List<String> enumSetToNames(java.util.Set<? extends Enum<?>> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.List<String> result = new java.util.ArrayList<String>(values.size());
        for (Enum<?> value : values) {
            result.add(value.name());
        }
        return result;
    }

    private static java.util.List<Map<String, Object>> normalizeDescriptors(java.util.List<ExternalProcessExtensionDescriptor> descriptors) {
        if (descriptors == null) {
            return Collections.emptyList();
        }
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<Map<String, Object>>(descriptors.size());
        for (ExternalProcessExtensionDescriptor descriptor : descriptors) {
            Map<String, Object> payload = new HashMap<String, Object>(DESCRIPTOR_PAYLOAD_CAPACITY);
            payload.put("className", descriptor.getClassName());
            payload.put("descriptor", normalizeDescriptorPayload(descriptor.getDescriptor()));
            result.add(payload);
        }
        return result;
    }

    private static Map<String, Object> normalizeDescriptorPayload(Map<String, Object> payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }
        if (JsonSafeValueNormalizer.isNormalizedMap(payload)) {
            return payload;
        }
        return JsonSafeValueNormalizer.normalizeMap(payload);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<ExternalProcessExtensionDescriptor> descriptorListValue(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        java.util.List<Map<String, Object>> raw = (java.util.List<Map<String, Object>>) value;
        java.util.List<ExternalProcessExtensionDescriptor> result = new java.util.ArrayList<ExternalProcessExtensionDescriptor>(raw.size());
        for (Map<String, Object> item : raw) {
            result.add(new ExternalProcessExtensionDescriptor(
                    stringValue(item.get("className")),
                    mapValue(item.get("descriptor"))
            ));
        }
        return result;
    }
}
