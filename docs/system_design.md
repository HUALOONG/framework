# P1-004c Redis 集成测试 — 系统架构设计

> 作者：高见远（架构师）/ Bob  
> 日期：2026-09  
> 目标：为 Jowen Framework 在**真实 Upstash Redis** 上的语义覆盖能力建立集成测试体系

---

## Part A: System Design

### 1. 技术难点与解决方案

| 难点 | 解决方案 |
|------|----------|
| **无 Docker/Testcontainers**，必须直连真实 Redis | 通过 `@EnabledIfSystemProperty("test.redis.enabled")` 条件激活，默认关闭 |
| **密码不能硬编码** | 使用 `System.getProperty("test.redis.url")` 注入，符合 Spring Boot 惯例 |
| **Upstash 可能 5s+ 延迟** | 所有 `RedissonClient` 操作超时设为 30s；测试用例用 `@Timeout(60)` |
| **并发线程安全** | 单线程测试 + `AtomicInteger` 用于并发幂等性测试；避免共享 Redisson 连接冲突 |
| **测试可重入** | `@BeforeEach` 清理 `jowen:test:integration:*` 前缀下的所有 key |
| **零新依赖** | 只复用 `redisson 4.7.0` + 现有 `spring-boot-starter-test` |
| **CI 隔离** | `@Tag("integration")` 标记，surefire 默认不选 |
| **测试分类** | `IntegrationTest` 基类统一处理连接和清理 |

### 2. 文件列表

```
framework-boot/framework-boot-web/src/test/java/cn/jowen/framework/extras/web/
├── lock/
│   └── RedissonCommandExecutorIntegrationTest.java    # P0：集成测试基类 + 基础 CRUD
├── ratelimit/
│   └── RedisFixedWindowRateLimiterIntegrationTest.java  # P0：固定窗口限流真实 Redis
│   └── RedisTokenBucketRateLimiterIntegrationTest.java  # P0：令牌桶限流真实 Redis
├── idempotent/
│   └── RedisIdempotentStoreIntegrationTest.java       # P0：幂等指纹跨实例语义
└── captcha/
    └── RedisCaptchaStoreIntegrationTest.java          # P1：验证码存储真实 Redis

framework-boot/framework-boot-autoconfigure/src/test/resources/
└── integration-test.properties                        # P0：测试配置（URL 等）
```

### 3. 数据结构和接口

```mermaid
classDiagram
    class IntegrationTest {
        +String redisUrl
        +RedissonClient redissonClient
        +RedisCommandExecutor executor
        +String keyPrefix
        +void setUp()
        +void tearDown()
        +void cleanIntegrationKeys()
    }

    class RedissonCommandExecutorIntegrationTest {
        +void testSetIfAbsent_andGet()
        +void testDelete()
        +void testDeleteIfMatch()
        +void testEval_LuaScript()
        +void testConcurrentSetIfAbsent()
    }

    class RedisFixedWindowRateLimiterIntegrationTest {
        +void testAllowUpToPermits()
        +void testRejectAfterPermits()
        +void testDistinctKeysIsolated()
    }

    class RedisTokenBucketRateLimiterIntegrationTest {
        +void testAllowUpToCapacity()
        +void testRejectAtCapacity()
        +void testRefillAfterWindow()
    }

    class RedisIdempotentStoreIntegrationTest {
        +void testTryMarkFirstSuccess()
        +void testTryMarkDuplicateRejected()
        +void testRemoveClearsMark()
        +void testCrossInstanceSemantics()
    }

    class RedisCaptchaStoreIntegrationTest {
        +void testSaveAndGetRoundTrip()
        +void testCustomKeyPrefix()
        +void testExpiredCaptchaRejected()
    }

    IntegrationTest <|-- RedissonCommandExecutorIntegrationTest
    IntegrationTest <|-- RedisFixedWindowRateLimiterIntegrationTest
    IntegrationTest <|-- RedisTokenBucketRateLimiterIntegrationTest
    IntegrationTest <|-- RedisIdempotentStoreIntegrationTest
    IntegrationTest <|-- RedisCaptchaStoreIntegrationTest
```

### 4. 程序调用流程

```mermaid
sequenceDiagram
    participant Test as Test Class
    participant Base as IntegrationTest
    participant Redis as Upstash Redis
    participant Executor as RedissonCommandExecutor

    Test->>Base: @BeforeEach setUp()
    Base->>Redis: connect(redisUrl)
    Redis-->>Base: connection established
    Base->>Base: cleanIntegrationKeys()
    Note over Base: DEL jowen:test:integration:*

    Test->>Base: testMethod()
    Base->>Executor: setIfAbsent(key, value, ttl)
    Executor->>Redis: SET key value EX ttl NX
    Redis-->>Executor: true
    Executor-->>Base: true
    Base->>Executor: get(key)
    Executor->>Redis: GET key
    Redis-->>Executor: "value"
    Executor-->>Base: "value"
    Base->>Base: assertThat(value).isEqualTo("value")

    Test->>Base: @AfterEach tearDown()
    Base->>Redis: quit/close
    Redis-->>Base: disconnected
```

### 5. 不确定事项与假设

| 项目 | 说明 |
|------|------|
| Upstash Redis 版本 | 假设支持标准 Redis 命令（GET/SET/DEL/EVAL）；若不支持 EVAL，脚本测试跳过 |
| 测试网络稳定性 | 假设测试期间网络稳定；添加 `@DisabledIf` 处理网络失败 |
| 端口和网络访问 | 假设 6379 端口可访问；若被封，测试自动跳过 |
| key 清理脚本 | Upstash 不支持 `SCAN` + `DEL`，改用 `EVAL` Lua 脚本清理前缀 key |

---

## Part B: Task Decomposition

### 6. Required Packages

```
- org.redisson:redisson:4.7.0         # Redis 客户端（已有）
- org.junit.jupiter:junit-jupiter     # 测试框架（已有）
- org.assertj:assertj-core            # 断言库（已有）
- org.mockito:mockito-junit-jupiter   # Mock 框架（已有）
- org.springframework.boot:spring-boot-test  # Spring Boot 测试（已有）
```

### 7. Task List（有序依赖）

| Task ID | Task Name | Source Files | Dependencies | Priority |
|---------|-----------|--------------|--------------|----------|
| T01 | 项目基础设施 + 集成测试基类 | `pom.xml` (test scope), `IntegrationTest.java`, `integration-test.properties` | - | P0 |
| T02 | RedissonCommandExecutor 基础 CRUD 集成测试 | `RedissonCommandExecutorIntegrationTest.java` | T01 | P0 |
| T03 | 限流器真实 Redis 集成测试 | `RedisFixedWindowRateLimiterIntegrationTest.java`, `RedisTokenBucketRateLimiterIntegrationTest.java` | T01, T02 | P0 |
| T04 | 幂等和验证码集成测试 | `RedisIdempotentStoreIntegrationTest.java`, `RedisCaptchaStoreIntegrationTest.java` | T01, T02 | P1 |

### 8. 共享知识

```
- 所有集成测试使用 @Tag("integration") 标记
- Redis URL 通过 System.getProperty("test.redis.url") 注入
- 测试前缀: "jowen:test:integration:"
- 所有操作超时: 30s
- 测试用例超时: 60s
- 使用 @EnabledIfSystemProperty("test.redis.enabled") 条件激活
- 清理脚本通过 Lua EVAL 实现
- 依赖 RedissonClient 单例，线程安全
```

### 9. 任务依赖图

```mermaid
graph TD
    T01[项目基础设施 + 集成测试基类] --> T02[RedissonCommandExecutor CRUD 测试]
    T01 --> T03[限流器集成测试]
    T01 --> T04[幂等和验证码集成测试]
    T02 --> T03
    T02 --> T04
    
    style T01 fill:#f9f,stroke:#333,stroke-width:4px
    style T02 fill:#bbf,stroke:#333,stroke-width:2px
    style T03 fill:#bbf,stroke:#333,stroke-width:2px
    style T04 fill:#bbf,stroke:#333,stroke-width:2px
```

---

## 关键设计决策说明

### Redisson 连接策略

**单连接 + 共享客户端**：所有测试共享同一个 `RedissonClient` 实例（在 `IntegrationTest` 基类的 `@BeforeEach` 中创建，`@AfterEach` 中关闭）。理由：
- 测试串行执行，无并发竞争
- 避免频繁创建/销毁连接的开销
- Upstash 连接建立有延迟，共享更安全

### 密钥注入方式

```java
// 禁止硬编码
@Value("${test.redis.url}")
private String redisUrl;

// 或在测试启动参数中传入
// -Dtest.redis.url=redis://default:password@host:port
// -Dtest.redis.enabled=true
```

### 幂等性保证

```java
@BeforeEach
void setUp() {
    cleanIntegrationKeys();  // 清理所有测试 key
}

@AfterEach
void tearDown() {
    RedisUtils.closeSilently(redissonClient);
}

private void cleanIntegrationKeys() {
    // 使用 Lua 脚本清理前缀 key
    String script = "local keys = redis.call('keys', ARGV[1]) " +
                    "for i=1,#keys,5000 do redis.call('del', unpack(keys, i, math.min(i+4999, #keys))) end " +
                    "return #keys";
    redissonClient.getScript().eval(RScript.Mode.READ_WRITE, script, 
        RScript.ReturnType.LONG, List.of(), List.of(keyPrefix + "*"));
}
```

### 测试分类标记

```java
@Tag("integration")
@EnabledIfSystemProperty(named = "test.redis.enabled", matches = "true")
@Timeout(60)
class RedissonCommandExecutorIntegrationTest extends IntegrationTest {
    // ...
}
```

### CI 友好性

```xml
<!-- surefire 配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

用户主动触发：
```bash
mvn test -Dtags=integration -Dtest.redis.enabled=true -Dtest.redis.url=...
```

---

## 测试用例设计

### RedissonCommandExecutorIntegrationTest

| 用例 ID | 测试方法 | 验证点 |
|---------|---------|--------|
| RCE-01 | `testSetIfAbsentAndGet` | `setIfAbsent` 成功 → `get` 返回相同值 |
| RCE-02 | `testSetIfAbsentDuplicate` | 重复 `setIfAbsent` 返回 false，值不变 |
| RCE-03 | `testDelete` | `delete` 后 `get` 返回 null |
| RCE-04 | `testDeleteIfMatch` | 值匹配时删除成功，不匹配时返回 false |
| RCE-05 | `testEvalLuaScript` | `incr` 脚本原子递增 |
| RCE-06 | `testTTL` | 设置 TTL 后 key 过期自动消失 |
| RCE-07 | `testConcurrentSetIfAbsent` | 多线程竞争，只有第一个成功 |

### RedisFixedWindowRateLimiterIntegrationTest

| 用例 ID | 测试方法 | 验证点 |
|---------|---------|--------|
| RFL-01 | `testAllowUpToPermits` | N 次请求内全部放行 |
| RFL-02 | `testRejectAfterPermits` | 超过配额后拒绝 |
| RFL-03 | `testDistinctKeysIsolated` | 不同 key 互不影响 |
| RFL-04 | `testWindowReset` | 窗口过期后计数重置 |

### RedisTokenBucketRateLimiterIntegrationTest

| 用例 ID | 测试方法 | 验证点 |
|---------|---------|--------|
| RTB-01 | `testAllowUpToCapacity` | 容量内全部放行 |
| RTB-02 | `testRejectAtCapacity` | 满桶时拒绝 |
| RTB-03 | `testRefillAfterWindow` | 窗口到期后令牌补充 |

### RedisIdempotentStoreIntegrationTest

| 用例 ID | 测试方法 | 验证点 |
|---------|---------|--------|
| RID-01 | `testTryMarkFirstSuccess` | 首次写入成功 |
| RID-02 | `testTryMarkDuplicateRejected` | 重复写入被拒绝 |
| RID-03 | `testRemoveClearsMark` | `remove` 后允许再次写入 |
| RID-04 | `testCrossInstanceSemantics` | 模拟多实例共享（通过直接操作 Redis） |

### RedisCaptchaStoreIntegrationTest

| 用例 ID | 测试方法 | 验证点 |
|---------|---------|--------|
| RCT-01 | `testSaveAndGetRoundTrip` | 保存后读出字段一致 |
| RCT-02 | `testCustomKeyPrefix` | 自定义前缀生效 |
| RCT-03 | `testExpiredCaptchaRejected` | 已过期验证码拒绝写入 |

---

## 运行方式

### 本地运行
```bash
mvn test -pl framework-boot/framework-boot-web \
  -Dtest="*IntegrationTest" \
  -Dtest.redis.enabled=true \
  -Dtest.redis.url=redis://default:PASSWORD@host:port
```

### CI 运行
```bash
mvn verify -Pintegration-tests \
  -Dtags=integration \
  -Dtest.redis.url=${REDIS_URL}
```

---

## 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| Upstash 网络抖动 | 中 | 重试机制 + 超时容忍 |
| 测试间互相干扰 | 低 | 严格清理 + 唯一 key 前缀 |
| 密钥泄露 | 高 | 密钥不入代码，通过环境变量注入 |
| 测试耗时过长 | 中 | 并行测试 + 超时控制 |

---

**设计完成。文档路径：`docs/system_design.md`**
