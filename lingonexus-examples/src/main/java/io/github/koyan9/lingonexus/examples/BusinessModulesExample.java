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

package io.github.koyan9.lingonexus.examples;

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务模块使用示例
 *
 * <p>演示如何使用 LingoNexus 内置的工具模块和业务模块</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class BusinessModulesExample {

    public static void main(String[] args) {
        // 创建引擎配置
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder().build())
                .build();

        // 创建脚本引擎并注册所有模块
        LingoNexusExecutor engine = LingoNexusBuilder.loadInstance(config);

        System.out.println("=== Business Modules Example ===\n");

        // 1. 数学模块 (math)
        System.out.println("--- Math Module ---");
        Map<String, Object> empty = new HashMap<>();
        System.out.println("math.add(0.1, 0.2) = " +
                engine.execute("math.add(0.1, 0.2)", "groovy", ScriptContext.of(empty)));
        System.out.println("math.scale(3.14159, 2, 'HALF_UP') = " +
                engine.execute("math.scale(3.14159, 2, 'HALF_UP')", "groovy", ScriptContext.of(empty)));
        System.out.println("math.pow(2, 10) = " +
                engine.execute("math.pow(2, 10)", "groovy", ScriptContext.of(empty)));

        // 2. 字符串模块 (str)
        System.out.println("\n--- String Module ---");
        Map<String, Object> strVars = new HashMap<>();
        strVars.put("text", "hello,world,foo,bar");
        System.out.println("str.split('hello,world,foo,bar', ',') = " +
                engine.execute("str.split(text, ',')", "groovy", ScriptContext.of(strVars)));
        System.out.println("str.join(['a','b','c'], '-') = " +
                engine.execute("str.join(['a','b','c'], '-')", "groovy", ScriptContext.of(empty)));

        // 3. 日期模块 (date)
        System.out.println("\n--- Date Module ---");
        System.out.println("date.now('yyyy-MM-dd HH:mm:ss') = " +
                engine.execute("date.now('yyyy-MM-dd HH:mm:ss')", "groovy", ScriptContext.of(empty)));
        System.out.println("date.timestamp() = " +
                engine.execute("date.timestamp()", "groovy", ScriptContext.of(empty)));

        // 4. JSON模块 (json)
        System.out.println("\n--- JSON Module ---");
        Map<String, Object> jsonVars = new HashMap<>();
        jsonVars.put("data", Map.of("name", "Alice", "age", 25));
        System.out.println("json.toJson(data) = " +
                engine.execute("json.toJson(data)", "groovy", ScriptContext.of(jsonVars)));

        jsonVars.put("jsonStr", "{\"user\":{\"name\":\"Bob\",\"city\":\"Beijing\"}}");
        System.out.println("json.parsePath(jsonStr, 'user.name') = " +
                engine.execute("json.parsePath(jsonStr, 'user.name')", "groovy", ScriptContext.of(jsonVars)));

        // 5. 集合模块 (col)
        System.out.println("\n--- Collection Module ---");
        Map<String, Object> colVars = new HashMap<>();
        colVars.put("list", java.util.List.of(1, 2, 3, 4, 5));
        System.out.println("col.size([1,2,3,4,5]) = " +
                engine.execute("col.size(list)", "groovy", ScriptContext.of(colVars)));
        System.out.println("col.first([1,2,3,4,5]) = " +
                engine.execute("col.first(list)", "groovy", ScriptContext.of(colVars)));
        System.out.println("col.last([1,2,3,4,5]) = " +
                engine.execute("col.last(list)", "groovy", ScriptContext.of(colVars)));

        // 6. 类型转换模块 (convert)
        System.out.println("\n--- Convert Module ---");
        System.out.println("convert.toInteger('42') = " +
                engine.execute("convert.toInteger('42')", "groovy", ScriptContext.of(empty)));
        System.out.println("convert.toBoolean('true') = " +
                engine.execute("convert.toBoolean('true')", "groovy", ScriptContext.of(empty)));
        System.out.println("convert.toString(12345) = " +
                engine.execute("convert.toString(12345)", "groovy", ScriptContext.of(empty)));

        // 7. 验证模块 (validator)
        System.out.println("\n--- Validator Module ---");
        System.out.println("validator.isEmail('test@example.com') = " +
                engine.execute("validator.isEmail('test@example.com')", "groovy", ScriptContext.of(empty)));
        System.out.println("validator.isPhone('13800138000') = " +
                engine.execute("validator.isPhone('13800138000')", "groovy", ScriptContext.of(empty)));
        System.out.println("validator.isUrl('https://example.com') = " +
                engine.execute("validator.isUrl('https://example.com')", "groovy", ScriptContext.of(empty)));
        System.out.println("validator.range(5, 1, 10) = " +
                engine.execute("validator.range(5, 1, 10)", "groovy", ScriptContext.of(empty)));

        // 8. 格式化模块 (formatter)
        System.out.println("\n--- Formatter Module ---");
        System.out.println("formatter.currency(1234567.89) = " +
                engine.execute("formatter.currency(1234567.89)", "groovy", ScriptContext.of(empty)));
        System.out.println("formatter.percent(0.856) = " +
                engine.execute("formatter.percent(0.856)", "groovy", ScriptContext.of(empty)));
        System.out.println("formatter.mask('13800138000', 3, 7) = " +
                engine.execute("formatter.mask('13800138000', 3, 7)", "groovy", ScriptContext.of(empty)));
        System.out.println("formatter.padLeft('42', 5, '0') = " +
                engine.execute("formatter.padLeft('42', 5, '0' as char)", "groovy", ScriptContext.of(empty)));

        // 9. 编码模块 (codec)
        System.out.println("\n--- Codec Module ---");
        System.out.println("codec.base64Encode('Hello') = " +
                engine.execute("codec.base64Encode('Hello')", "groovy", ScriptContext.of(empty)));
        System.out.println("codec.base64Decode('SGVsbG8=') = " +
                engine.execute("codec.base64Decode('SGVsbG8=')", "groovy", ScriptContext.of(empty)));
        System.out.println("codec.md5('hello') = " +
                engine.execute("codec.md5('hello')", "groovy", ScriptContext.of(empty)));
        System.out.println("codec.uuid() = " +
                engine.execute("codec.uuid()", "groovy", ScriptContext.of(empty)));

        // 10. 综合业务场景：订单处理
        System.out.println("\n--- Business Scenario: Order Processing ---");
        String orderScript = """
                // 验证输入
                if (!validator.isEmail(email)) {
                    return [success: false, error: 'Invalid email']
                }
                if (!validator.isPhone(phone)) {
                    return [success: false, error: 'Invalid phone']
                }
                
                // 计算价格
                def subtotal = math.multiply(price, quantity)
                def discount = subtotal > 1000 ? 0.1 : 0
                def finalPrice = math.scale(math.multiply(subtotal, 1 - discount), 2, 'HALF_UP')
                
                // 生成订单
                def order = [
                    orderId: codec.uuid(),
                    customer: [
                        email: email,
                        phone: formatter.mask(phone, 3, 7)
                    ],
                    amount: formatter.currency(finalPrice),
                    discount: formatter.percent(discount),
                    createdAt: date.now('yyyy-MM-dd HH:mm:ss')
                ]
                
                return [success: true, order: order]
                """;

        Map<String, Object> orderVars = new HashMap<>();
        orderVars.put("email", "customer@example.com");
        orderVars.put("phone", "13800138000");
        orderVars.put("price", 299.99);
        orderVars.put("quantity", 5);

        Object orderResult = engine.execute(orderScript, "groovy", ScriptContext.of(orderVars));
        System.out.println("Order Result: " + orderResult);

        System.out.println("\n=== Example Completed ===");
    }
}
