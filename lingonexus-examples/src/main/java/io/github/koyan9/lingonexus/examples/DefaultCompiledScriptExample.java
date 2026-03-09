package io.github.koyan9.lingonexus.examples;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.compiled.DefaultCompiledScript;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;

/**
 * DefaultCompiledScript 使用示例
 *
 * <p>展示如何使用 DefaultCompiledScript 实现编译和执行分离</p>
 */
public class DefaultCompiledScriptExample {

    /**
     * 模拟一个简单的 Evaluator
     */
    static class SimpleEvaluator {
        private String compiledCode;

        public void cook(String script) {
            System.out.println("Compiling script: " + script);
            this.compiledCode = "COMPILED[" + script + "]";
        }

        public Object evaluate(Object[] params) {
            System.out.println("Executing with params: " + java.util.Arrays.toString(params));
            return compiledCode + " result=" + (params.length > 0 ? params[0] : "none");
        }
    }

    public static void main(String[] args) {
        String script = "x * 2";
        ScriptContext compileContext = ScriptContext.builder()
                .put("x", 10)
                .build();

        // 创建 CompiledScript - 此时自动触发编译
        System.out.println("=== 创建 CompiledScript (自动触发编译) ===");
        CompiledScript compiled = new DefaultCompiledScript(
                ScriptLanguage.JAVA,
                script,
                compileContext,
                // 编译函数 - 类似 evaluator.cook()
                (src, ctx) -> {
                    SimpleEvaluator evaluator = new SimpleEvaluator();
                    evaluator.cook(src);  // 真正的编译在这里
                    return evaluator;
                },
                // 执行函数
                (compiledObj, ctx) -> {
                    SimpleEvaluator evaluator = (SimpleEvaluator) compiledObj;
                    Object[] params = ctx.getVariables().values().toArray();
                    return evaluator.evaluate(params);
                }
        );

        System.out.println("\n=== 执行编译后的脚本 ===");
        // 执行多次，使用不同的上下文
        ScriptContext execContext1 = ScriptContext.builder()
                .put("x", 10)
                .build();
        Object result1 = compiled.execute(execContext1);
        System.out.println("Result 1: " + result1);

        ScriptContext execContext2 = ScriptContext.builder()
                .put("x", 20)
                .build();
        Object result2 = compiled.execute(execContext2);
        System.out.println("Result 2: " + result2);

        System.out.println("\n=== 脚本信息 ===");
        System.out.println("Language: " + compiled.getLanguage());
        System.out.println("Source: " + compiled.getSource());
    }
}
