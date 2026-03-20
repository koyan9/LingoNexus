# External Summary (2026-03-20)

Short copy-paste summary for announcements or status updates.

## English

LingoNexus update (2026-03-20):
- `EXTERNAL_PROCESS` diagnostics are now harder to misread: request, handshake, borrow, and worker failures are tracked more consistently.
- Direct-path hot-path work has reduced avoidable preparation and metadata-plan overhead.
- The default performance suite now includes metadata-profile, context-source, module-usage, and failure-diagnostics comparisons.

Current performance snapshot (environment-specific, use for trend comparison):
- External-process reuse: cold `770 ms`, repeated avg `2.00 ms`
- Isolation mode comparison (repeated avg): `DIRECT 0.40 ms`, `ISOLATED_THREAD 0.60 ms`, `EXTERNAL_PROCESS 2.45 ms`
- Large-context comparison (repeated avg): `DIRECT 0.20 ms`, `ISOLATED_THREAD 0.50 ms`, `EXTERNAL_PROCESS 4.00 ms`

## 中文

LingoNexus 更新（2026-03-20）：
- `EXTERNAL_PROCESS` 的诊断路径更稳定了，请求校验、握手、借用 worker、worker 执行这几类失败现在更容易区分。
- direct-path 热路径已经减少了一部分不必要的准备和 metadata-plan 开销。
- 默认性能基线套件现在已经覆盖 metadata profile、上下文来源、模块使用复杂度和失败诊断成本对比。

当前性能快照（与环境相关，仅用于趋势对比）：
- 外部进程复用：冷启动 `770 ms`，重复均值 `2.00 ms`
- 隔离模式对比（重复均值）：`DIRECT 0.40 ms`，`ISOLATED_THREAD 0.60 ms`，`EXTERNAL_PROCESS 2.45 ms`
- 大上下文对比（重复均值）：`DIRECT 0.20 ms`，`ISOLATED_THREAD 0.50 ms`，`EXTERNAL_PROCESS 4.00 ms`
