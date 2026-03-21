# PR 摘要（2026-03-20）

## 建议标题

`稳定外部进程诊断、优化热路径，并扩展性能基线`

## 短版说明

这批改动主要完成了四件事：

1. 扩展 Spring Boot 配置面  
   新增 sandbox selection / transport 相关配置绑定，并补齐配置验证。

2. 加强 `EXTERNAL_PROCESS` 诊断  
   请求校验、握手、借用 worker、worker 执行几类失败现在更容易区分；聚合计数和 latest snapshot 也更一致。

3. 优化 direct-path 热路径  
   去掉一部分不必要的 merge/copy，并缓存 metadata-plan / isolation-mode 这类可预计算信息。

4. 扩展性能基线与文档入口  
   默认 baseline 套件已覆盖：
   - metadata profile
   - context source
   - module usage
   - failure diagnostics

## 当前验证

已通过：

- `mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am test -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -q -pl lingonexus-testcase/lingonexus-testcase-springboot -am test -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -q -pl lingonexus-examples -am -DskipTests compile`
- `mvn -q test -Dsurefire.failIfNoSpecifiedTests=false`
- `powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -SkipVerifiedBuild`

## 当前性能快照

基于 `docs/performance-reports/latest-performance-report.md`（2026-03-19 21:51:43）：

- 外部进程复用：重复均值 `2.00 ms`
- 隔离模式对比：`DIRECT 0.40 ms` / `ISOLATED_THREAD 0.60 ms` / `EXTERNAL_PROCESS 2.45 ms`
- 大上下文对比：`DIRECT 0.20 ms` / `ISOLATED_THREAD 0.50 ms` / `EXTERNAL_PROCESS 4.00 ms`

这些数字与环境相关，只用于趋势对比，不作为固定目标。

## 风险说明

- `EXTERNAL_PROCESS` 仍然更适合 JSON-safe 的变量、metadata 和结果值
- 自定义 `SecurityPolicy` 跨 worker 的兼容性已增强诊断可见性，但并非完全无约束
- 大上下文和外部进程相关数值对本机波动仍较敏感
