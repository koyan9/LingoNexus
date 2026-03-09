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
package io.github.koyan9.lingonexus.testcase.nospring;

final class BenchmarkReportSupport {

    private BenchmarkReportSupport() {
    }

    static void emit(String benchmarkName, Object... keyValues) {
        StringBuilder line = new StringBuilder("BENCHMARK|");
        line.append(sanitize(benchmarkName));
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            line.append('|')
                    .append(sanitize(keyValues[i]))
                    .append('=')
                    .append(sanitize(keyValues[i + 1]));
        }
        System.out.println(line.toString());
    }

    private static String sanitize(Object value) {
        return String.valueOf(value)
                .replace('|', '/')
                .replace((char) 13, ' ')
                .replace((char) 10, ' ')
                .trim();
    }
}
