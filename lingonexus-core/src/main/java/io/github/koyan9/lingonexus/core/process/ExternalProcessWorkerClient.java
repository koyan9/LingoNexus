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

import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.exception.ScriptTimeoutException;
import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent worker client backed by one external JVM process.
 */
public class ExternalProcessWorkerClient implements LifecycleAware {

    private static final int STARTUP_TIMEOUT_MS = 10000;

    private final Process process;
    private final Socket socket;
    private final BufferedOutputStream requestStream;
    private final BufferedInputStream responseStream;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong lastReturnedAtNanos = new AtomicLong(System.nanoTime());
    private volatile ExternalProcessWorkerPool.ProtocolHandshakeSnapshot protocolHandshakeSnapshot =
            ExternalProcessWorkerPool.ProtocolHandshakeSnapshot.empty();
    private volatile String protocolVersion;
    private volatile List<String> supportedTransportProtocolCapabilities = Collections.emptyList();
    private volatile List<String> supportedTransportSerializerContractIds = Collections.emptyList();

    protected ExternalProcessWorkerClient() {
        this.process = null;
        this.socket = null;
        this.requestStream = null;
        this.responseStream = null;
    }

    public ExternalProcessWorkerClient(String javaCommand, String classpath) throws IOException {
        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        serverSocket.setSoTimeout(STARTUP_TIMEOUT_MS);
        try {
            this.process = new ProcessBuilder(
                    javaCommand,
                    "-cp",
                    classpath,
                    ExternalProcessWorkerMain.class.getName(),
                    "--socket",
                    String.valueOf(serverSocket.getLocalPort())
            )
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            this.socket = serverSocket.accept();
            this.socket.setTcpNoDelay(true);

            this.requestStream = new BufferedOutputStream(socket.getOutputStream());
            this.responseStream = new BufferedInputStream(socket.getInputStream());
        } catch (IOException e) {
            serverSocket.close();
            throw e;
        } finally {
            serverSocket.close();
        }
    }

    public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
        ensureActive();
        try {
            socket.setSoTimeout(timeoutMs > 0 ? safeSocketTimeout(timeoutMs) : 0);
            ExternalProcessProtocolCodec.writeRequest(requestStream, request);
            return ExternalProcessProtocolCodec.readResponse(responseStream);
        } catch (SocketTimeoutException e) {
            shutdown();
            throw new ScriptTimeoutException(timeoutMs, timeoutMs);
        } catch (EOFException e) {
            shutdown();
            throw new ScriptRuntimeException("External worker terminated unexpectedly", e);
        } catch (Exception e) {
            shutdown();
            throw new ScriptRuntimeException("External worker execution failed", e);
        }
    }

    public boolean isAlive() {
        return !isShutdown() && process != null && process.isAlive() && socket != null && !socket.isClosed();
    }

    public void markBorrowed() {
        lastReturnedAtNanos.set(0L);
    }

    public void markReturned() {
        lastReturnedAtNanos.set(System.nanoTime());
    }

    public boolean isIdleExpired(long idleTtlMs) {
        if (idleTtlMs <= 0) {
            return false;
        }
        long returnedAt = lastReturnedAtNanos.get();
        return returnedAt > 0L && (System.nanoTime() - returnedAt) > java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(idleTtlMs);
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

    ExternalProcessWorkerPool.ProtocolHandshakeSnapshot getProtocolHandshakeSnapshot() {
        return protocolHandshakeSnapshot;
    }

    public synchronized boolean ping() {
        if (!isAlive()) {
            return false;
        }
        try {
            ExternalProcessExecutionResponse response = execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);
            if (response.isSuccess() && Boolean.TRUE.equals(response.getValue())) {
                protocolVersion = response.getProtocolVersion();
                supportedTransportProtocolCapabilities = response.getSupportedTransportProtocolCapabilities() != null ? response.getSupportedTransportProtocolCapabilities() : Collections.<String>emptyList();
                supportedTransportSerializerContractIds = response.getSupportedTransportSerializerContractIds() != null ? response.getSupportedTransportSerializerContractIds() : Collections.<String>emptyList();
                protocolHandshakeSnapshot = ExternalProcessWorkerPool.ProtocolHandshakeSnapshot.of(
                        protocolVersion,
                        supportedTransportProtocolCapabilities,
                        supportedTransportSerializerContractIds
                );
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        closeQuietly(responseStream);
        closeQuietly(requestStream);
        closeQuietly(socket);
        if (process != null) {
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    private void ensureActive() {
        if (!isAlive()) {
            throw new ScriptRuntimeException("External worker is not available");
        }
    }

    private int safeSocketTimeout(long timeoutMs) {
        return timeoutMs >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) timeoutMs;
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
