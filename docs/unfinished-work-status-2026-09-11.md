# Jowen Framework 未完成项现状盘点（2026-09-12 更新）

> 基线：HEAD `79e20d0`（`feat(ratelimit): P1-004b 滑动窗口 + 漏桶 Redis 集群化实现`）
> 验证：`mvn -o clean verify`（BUILD SUCCESS，`Rule violated` = 0）

---

## 一、结论先行

**剩余未完成项：2 项**（09-05 审计报告列出 5 项，其中 N-003 已按 09-07 决策废弃处理、P2-004b 已完结、P1-004b 已于本次落地）。无 P0 级阻塞项；**剩余两项均为 P2 级或非阻塞项**。

本轮（09-11 → 09-12）新增且已完成：
- P1-004b 限流集群化：滑动窗口（ZSET）+ 漏桶（HMSET 水位）Redis 实现补齐，四种算法全覆盖
- N-003 盘点报告纠正（保留不删，与 09-07 正式决策对齐）
- boot-web README §8 三行过时状态修正

---

## 二、逐项核实（逐条复核代码，非查旧文档）

### ✅ P1-004b — 限流集群化（**已完成**）

**现状**：四种算法全部有 Redis 集群化实现。
- `FixedWindowRateLimiter` / `TokenBucketRateLimiter`：原有 Redis 实现（`RedisFixedWindowRateLimiter` / `RedisTokenBucketRateLimiter`）
- `SlidingWindowRateLimiter`：新增 `RedisSlidingWindowRateLimiter`（ZADD + ZREMRANGEBYSCORE + ZCARD 原子脚本）
- `LeakyBucketRateLimiter`：新增 `RedisLeakyBucketRateLimiter`（HMGET/HSET 持久化水位，Lua 原子计算）

**关键设计**：
- `RateLimitScripts` 新增 `SLIDING_WINDOW` / `LEAKY_BUCKET` 常量，标记与现有脚本一致
- `RateLimitKeys` 新增 `slidingWindow()` / `leakyBucket()` key 拼接方法（`sw:` / `lb:` 前缀，不与 `fw:` / `tb:` 冲突）
- `RateLimiterManager.createByAlgorithm` 路由四算法全覆盖（`supportsScript() && keys != null` 时全部走 Redis）
- FakeRedisCommandExecutor 内置两种新脚本的 Java 等价实现，离线测试完整走通脚本链路
- 构造期强制 `executor.supportsScript() == true`；运行期 Redis 异常按 `failOpen`（默认）/ `failClosed` 处置

**测试**：`RedisSlidingWindowRateLimiterTest`（6 用例）+ `RedisLeakyBucketRateLimiterTest`（6 用例）+ `RateLimiterManagerTest` 追加 5 用例 + `RateLimitScriptsTest` 追加 2 用例

### 🟡 P2-001 — message 7 个 sender 补实现（仍待办）

**现状核实**（`framework-extras-message/provider/`）：

| 类 | 状态 |
|---|---|
| `DingTalkMessageSender` | `abstract` 骨架 |
| `EmailMessageSender` | `abstract` 骨架 |
| `PushMessageSender` | `abstract` 骨架 |
| `SiteMessageSender` | `abstract` 骨架 |
| `SmsMessageSender` | `abstract` 骨架 |
| `WeComMessageSender` | `abstract` 骨架 |
| `WebhookMessageSender` | `abstract` 骨架 |
| **`HttpWebhookMessageSender`** | ✅ **已落地**（`extends WebhookMessageSender`，JDK 内置 `HttpClient`，JSON 信封/裸文本双模式） |

故"7 个骨架"的说法需修正：**6 个抽象骨架 + 1 个已落地实现**。
落地路径：Webhook 零外部依赖，可立即做；其余 6 个需各渠道 API 凭证与协议细节。

### 🟢 N-003 — `Notification` 配置块空转（**已废弃，保留不删**）

**现状核实**：`ExtrasWebProperties.Notification` 仅作为 POJO 字段存在（L483 Javadoc 已自认"框架通知能力未落地"），全 `framework-boot-web` 无任何实现类或装配逻辑引用它。配置写入静默不生效。

**执行动作已完成**：`NotificationProperties` 与 `ExtrasWebProperties.Notification` 已标 `@Deprecated` + Javadoc 说明；`NotificationException` / `E2004` 保留（仍被 `MessageServiceSmsCaptchaSender` 引用）。

`boot-web/README.md` §8 的状态行已修正为"✅ 已废弃（`@Deprecated`，计划 1.0 移除）"。

### 🟢 P2-004b — `docs/code-analysis.md` 过时（✅ 已完结）

**现状核实**：文件头部已加醒目标注——"历史快照（截至 2026-09-05）……已过时、仅供参考……请勿基于本文逐行更新现状文档"。534 行，状态已从"待办"转为"已标记"，无需再改。

---

## 三、本轮归零项

### P1-004b — 限流集群化（2026-09-12，`79e20d0`）

四种算法全覆盖 Redis 实现：
- `RateLimitScripts` 新增 `SLIDING_WINDOW`（ZSET 时间戳有序集合）和 `LEAKY_BUCKET`（HMSET 水位持久化）Lua 脚本
- `RateLimitKeys` 新增 `slidingWindow(dimKey)` / `leakyBucket(dimKey)` 拼接方法
- 新增 `RedisSlidingWindowRateLimiter` / `RedisLeakyBucketRateLimiter`，构造期强制 `supportsScript()==true`，运行期 failOpen/failClosed 可配
- `RateLimiterManager.createByAlgorithm` 路由扩展为四算法全覆盖
- `FakeRedisCommandExecutor` 内置两种新脚本的 Java 等价实现，离线测试完整走通
- 19 个新增测试用例全部通过

### N-003 盘点报告纠正（2026-09-12，`5b381d1`）

盘点报告建议"删除"与该事项的正式决策（`docs/design-rate-limit-cluster-2026-09-07.md` §8.4，`a0081be`）冲突——`E2004` 是协议级 API，删除属于协议 breaking。已修正盘点报告为"已废弃，保留不删"，同步修正 boot-web README §8 状态行。

### cache 的 `condition` / `unless` 条件评估

09-05 审计报告未将其列为独立项（当时评估为"骨架"），09-10 评估确认完成度约 10%。现已端到端落地，提交 `9dad88e`：

- `framework-cache/condition/ConditionEvaluator.java`：抽象类，零 Spring 依赖；`evaluate`（空/空白→恒成立）/ `evaluateUnless`（空/空白→恒不成立）默认方法 + 抽象 `doEvaluate(String, CacheOperationContext)`
- `framework-boot-autoconfigure/SpelConditionEvaluator.java`：SpEL 实现，注入 `#target` / `#a0` / `#p0` / 参数名变量；`toBoolean` 处理 Boolean/Number/String 转换
- `SpringCacheAnnotationProcessor`：三处 `@Around` 接入 `passesCondition`（condition 为假 → 只执行方法体、跳过缓存；unless 为真 → 跳过缓存；无 Bean → 回退无条件通过）；`@CacheEvict` 的 evict 加 `if (passesCondition(ctx))` 守卫
- 根 `pom.xml`：compiler 插件加 `<parameters>${maven.compiler.parameters}</parameters>`（此前属性声明了但未被引用，导致 `#id` 类参数名变量求值为 null）
- 测试：`ConditionEvaluatorTest`（抽象层契约）+ `SpelConditionEvaluatorTest`（11 用例）+ `SpringCacheAnnotationProcessorTest` 追加 10+ 条件用例

**踩坑**：`SpelConditionEvaluator.buildContext` 最初只注册 `#a0/#p0`，漏了参数名变量，导致 `#id == 'u-1'` 失败；用 `SpelProbeTest3` 隔离验证后补上。

### P1-004c — 真实 Redis 集成测试（设计文档已提交）

`docs/system_design.md`（347 行）+ `class-diagram.mermaid` / `sequence-diagram.mermaid` 已随 `9dad88e` 提交。
代码侧 `RedissonCommandExecutorIntegrationTest` 基类早前已落地（`@Tag("integration")`，需 `-Dtest.redis.url`，默认跳过，不阻塞门禁）。

**差距**：设计文档规划的 5 个测试类（`RedisFixedWindowRateLimiterIntegrationTest`、`RedisTokenBucketRateLimiterIntegrationTest`、`RedisIdempotentStoreIntegrationTest`、`RedisCaptchaStoreIntegrationTest`）尚未编写；当前仅基类 + 4 个 `Nested` 子套件（`SetIfAbsentAndGet` / `SupportsScript` / `DeleteIfMatch` / `Eval` / `Ttl` / `DistributedLockSemantics` / `Delete`，全部 skip）。

---

## 四、覆盖率门禁（独立口径，`mvn -o clean verify`）

| 模块 | 行覆盖率 | 备注 |
|---|---|---|
| framework-data/data-core | 100.00% | |
| framework-data/data-mybatis | 99.69% | |
| framework-data/data-jdbc | 98.69% | |
| framework-extras/extras-storage | 98.33% | |
| framework-boot/boot-web | 98.02% | P1-004b 新增 7 个类 19 个用例，覆盖完整 |
| framework-logger | 97.74% | |
| framework-extras/extras-common | 96.39% | |
| framework-plugin | 96.12% | |
| framework-core | 95.60% | |
| framework-boot/boot-autoconfigure | 95.60% | |
| framework-i18n | 94.89% | **余量最紧（0.49pt）** |
| framework-cache | 94.22% | 含 condition/unless SpEL 实现 |
| framework-extras/extras-message | 93.65% | |

13/13 全部通过门禁（BUILD SUCCESS + 14 个 "All coverage checks have been met"）。**最脆弱模块是 i18n（94.89%）**；后续在 `boot-autoconfigure` 新增 Bean 装配分支时必须同步补测试。

---

## 五、推进建议（按风险/价值排序）

```
第 1 步  🟡 P2-001 message sender — 先做 HttpWebhook 之外的渠道（Webhook 已落地）
         └─ 与 P1-004b/P1-004c 独立，可并行
第 2 步  🟢 P1-004c 真实 Redis 集成测试 — 补 4 个测试类
         └─ 需 Upstash 凭证；与 P2-001 独立
```

**无需再做**：
- P1-004b（已完成，`79e20d0`）
- P2-004b（`docs/code-analysis.md` 已标记为历史快照）
- N-003 已按 09-07 决策废弃处理（`a0081be` + `5b381d1`），**不再建议删除**
