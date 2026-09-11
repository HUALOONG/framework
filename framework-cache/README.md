# framework-cache 模块架构设计

> 文档元信息
> - **模块**：framework-cache
> - **关键词**：多级缓存、Caffeine、Redisson、缓存注解、穿透/雪崩/击穿防护、缓存锁
> - **描述**：多级缓存管理模块，提供本地缓存（Caffeine）、分布式缓存（Redisson）与组合缓存，支持缓存注解、事件监听、缓存锁与穿透/雪崩/击穿防护，仅依赖 framework-core
> - **基线**：Spring Boot 4.x + Java 21（Jackson 3、Micrometer 2.0、Actuator）

---

## 一、模块定位

`framework-cache` 是框架的 **多级缓存管理模块（L2）**，提供 **本地缓存（Caffeine）、分布式缓存（Redisson）以及多级缓存（Local + Remote 组合）** 等能力。模块定义了统一的缓存抽象层，支持缓存注解（@Cacheable、@CacheEvict、@CachePut）、缓存事件监听、缓存序列化、缓存锁等能力。模块不依赖 Spring Boot，保持核心抽象的纯净性，同时提供完善的缓存穿透、雪崩、击穿防护机制。

**核心价值**：

| 场景         | 没有本模块                                                   | 有本模块                                                             |
|:-------------|:-------------------------------------------------------------|:---------------------------------------------------------------------|
| 多级缓存管理 | 业务方需自行维护本地与分布式缓存的一致性，容易出现数据不一致 | 提供统一的 Local + Remote 多级缓存抽象，自动处理读写顺序与数据回填   |
| 缓存异常防护 | 缺乏统一的穿透、雪崩、击穿防护机制，需各业务自行实现         | 内置空值缓存、过期抖动、互斥锁等防护策略，开箱即用                   |
| 缓存序列化   | 各业务模块序列化方式不统一，导致缓存数据不兼容               | 提供可插拔的序列化 SPI，默认 Jackson，支持 Kryo 等高性能方案         |

**与核心模块的边界**：

| 模块                | 定位             | 特点                                                   |
|:--------------------|:-----------------|:-------------------------------------------------------|
| **framework-cache** | **多级缓存管理** | **统一缓存抽象、多级缓存、注解驱动、事件监听、缓存锁** |
| framework-core      | 基础设施         | SPI、异常、断言                                        |
| framework-data-*    | 数据访问         | 仓储、查询、事务                                       |

---

## 二、功能清单与依赖矩阵

| 功能                | 子包             | 核心依赖                        | 可选依赖            |
|:--------------------|:-----------------|:--------------------------------|:--------------------|
| 缓存核心接口        | api              | —                               | —                   |
| 缓存注解            | annotation       | framework-core                  | Spring AOP          |
| Caffeine 本地缓存   | cache/caffeine   | Caffeine                        | —                   |
| Redisson 分布式缓存 | cache/redisson   | Redisson                        | —                   |
| 多级缓存            | cache/multilevel | framework-core                  | —                   |
| 缓存事件            | event            | framework-core Event            | —                   |
| 缓存序列化          | serializer       | —                               | Jackson / Kryo      |
| 缓存键生成器        | support          | —                               | —                   |
| 缓存淘汰策略        | eviction         | —                               | —                   |
| 缓存锁              | lock             | Redisson                        | —                   |
| 配置属性            | config           | —                               | —                   |

---

## 三、整体包结构

```text
framework-cache
└─ src/main/java/cn/jowen/framework/cache/
   ├─ annotation/             # 缓存注解（@Cacheable、@CacheEvict、@CachePut、@EnableCaching）
   ├─ api/                    # 缓存核心接口（Cache、CacheManager、CacheConfiguration、CacheStats、NullValue）
   ├─ cache/                  # 缓存实现
   │  ├─ caffeine/            # Caffeine 本地缓存
   │  ├─ redisson/            # Redisson 分布式缓存
   │  └─ multilevel/          # 多级缓存（Local + Remote 组合）
   ├─ config/                 # 缓存配置属性（纯 POJO）
   ├─ event/                  # 缓存事件
   ├─ eviction/               # 淘汰策略（LRU 等）
   ├─ lock/                   # 缓存锁（Redisson 实现）
   ├─ serializer/             # 序列化（Jackson 默认）
   └─ support/                # 缓存键生成器、条件解析
```

> **注意**：`framework-cache/config/CacheProperties` 为纯 POJO（不含 Spring 注解）。自动装配类（`CacheAutoConfiguration`）统一由
> `framework-boot-autoconfigure` 模块的 `cache/` 子包承载。

---

## 四、各子包详细设计

#### 4.1 api/ — 缓存核心接口

##### 定位

定义缓存模块的顶层抽象，包括 `CacheManager`、`Cache`、`CacheConfiguration`、`CacheStats` 等核心接口，所有缓存实现均基于此包进行适配。

```textmate
cn.jowen.framework.cache.api
├─ CacheManager                    # 缓存管理器接口
│  ├─ <K,V> Cache<K,V> getCache(String name)     # 按名称获取或创建缓存
│  ├─ CacheStats getStats(String name)           # 获取缓存统计
│  ├─ Iterable<String> cacheNames()              # 返回所有缓存名
│  └─ void register(Cache<?,?> cache)            # 注册外部构建的缓存
├─ Cache<K,V>                      # 缓存操作接口
│  ├─ String name()
│  ├─ @Nullable V get(K key)
│  ├─ default Optional<V> getOptional(K key)
│  ├─ void put(K key, V value)
│  ├─ default void put(K key, V value, @Nullable Duration ttl)
│  ├─ boolean putIfAbsent(K key, V value)
│  ├─ void evict(K key)
│  ├─ void clear()
│  ├─ long size()
│  └─ CacheStats stats()
├─ CacheConfiguration              # 缓存配置（Builder 模式）
│  ├─ maxSize: long
│  ├─ expireAfterWrite: Duration
│  ├─ expireAfterAccess: Duration
│  ├─ serializer: CacheSerializer
│  ├─ evictionPolicy: EvictionPolicy
│  ├─ nullValueCache: boolean
│  ├─ jitter: Duration
│  ├─ mutexLock: boolean
│  └─ static Builder builder()
├─ CacheStats                      # 缓存统计（命中/未命中计数）
└─ NullValue                       # 空值标记，用于缓存穿透防护
   └─ static final NullValue INSTANCE
```

**使用示例**：

```textmate
// 获取缓存实例并操作
CacheManager cacheManager = new DefaultCacheManager();
Cache<String, User> userCache = cacheManager.getCache("user");

User user = userCache.get("user:1001");
if (user == null) {
    user = userRepository.findById(1001);
    if (user != null) {
        userCache.put("user:1001", user);
    }
}

// 配置化创建
CacheConfiguration config = CacheConfiguration.builder()
        .maxSize(10000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .nullValueCache(true)
        .jitter(Duration.ofSeconds(60))
        .mutexLock(true)
        .build();
```

---

#### 4.2 annotation/ — 缓存注解

##### 定位

提供声明式缓存能力，通过 AOP 拦截方法调用，自动完成缓存的读取、写入和清除。

```textmate
cn.jowen.framework.cache.annotation
├─ Cacheable                       # 缓存读取，命中则直接返回
│  ├─ value: String
│  ├─ key: String                  # 支持 SpEL
│  ├─ condition: String            # 条件表达式（SpEL）
│  ├─ unless: String               # 否定条件（SpEL）
│  ├─ sync: boolean                # 是否同步加载（默认 false）
│  └─ listener: String             # 事件监听器 Bean 名
├─ CacheEvict                      # 缓存清除
│  ├─ value: String
│  ├─ key: String
│  ├─ allEntries: boolean
│  └─ beforeInvocation: boolean
├─ CachePut                        # 缓存写入（不跳过方法执行）
│  ├─ value: String
│  ├─ key: String
│  ├─ condition: String
│  └─ unless: String
├─ EnableCaching                   # 启用缓存注解支持
│  └─ proxyTargetClass: boolean
└─ CacheAnnotationProcessor        # 注解处理器（AOP 切面）
   ├─ 解析 @Cacheable / @CacheEvict / @CachePut 注解
   ├─ 根据注解配置执行缓存操作
   └─ 支持条件表达式（SpEL）求值
```

**使用示例**：

```textmate

@Service
public class UserService {

    @Cacheable(value = "user", key = "#userId")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userRepository.update(user);
        return user;
    }

    @CacheEvict(value = "user", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Cacheable(value = "user", key = "#userId", condition = "#userId > 0")
    public User getUserConditional(Long userId) {
        return userRepository.findById(userId);
    }
}
```

**条件表达式语义**

`condition` 与 `unless` 均为 SpEL 表达式，语义与 Spring Framework 一致：

| 属性 | 为空/空白时 | 表达式为 `false` 时 | 表达式为 `true` 时 |
|---|---|---|---|
| `condition`（`@Cacheable` / `@CachePut`） | 视为**恒成立**，正常走缓存读写 | 跳过本次缓存操作，**方法体仍执行** | 正常走缓存读写 |
| `unless`（`@Cacheable` / `@CachePut`） | 视为**恒不成立**，正常走缓存读写 | 正常走缓存读写 | **跳过**本次缓存读写 |

- 两个属性同时存在时，`condition` 与 `unless` 需同时满足（`unless` 内部取反）才执行缓存操作。
- `@CacheEvict` **没有** `condition` 属性，与 Spring 一致，始终清除。
- 表达式可用的变量：`#target` 目标对象、`#a0` / `#p0` … 按索引的参数、以及按编译期参数名（需 `-parameters`）的变量，如 `#userId`。
- 扩展：`ConditionEvaluator` 是 SPI 接口，框架默认提供基于 Spring SpEL 的 `SpelConditionEvaluator`（位于 `framework-boot-autoconfigure` 模块，由 `conditionEvaluator` Bean 装配）；业务方可替换为 Aviator、QLExpress 等引擎，只需继承并实现 `doEvaluate`。

---

#### 4.3 cache/caffeine/ — Caffeine 本地缓存

##### 定位

基于 Caffeine 的高性能本地缓存实现，适用于单机场景或作为多级缓存的 L1 层。支持基于内存大小的淘汰、基于时间的过期、基于引用的淘汰等策略。

```textmate
cn.jowen.framework.cache.cache.caffeine
├─ CaffeineCache                                      # Caffeine Cache 适配器
│  ├─ delegate: Cache<Object, Object>                 # Caffeine Cache 实例
│  ├─ serializer: CacheSerializer                     # 序列化器
│  ├─ get(String key, Class<T> type) -> T
│  ├─ put(String key, Object value, Duration ttl)
│  ├─ evict(String key)
│  ├─ clear()
│  └─ stats() -> CacheStats
└─ CaffeineCacheManager                               # Caffeine CacheManager 实现
   ├─ createDefault() -> CacheManager
   ├─ create(String name, CacheConfiguration config) -> Cache
   └─ configure(CaffeineConfigurer configurer)
```

**使用示例**：

```textmate
CacheConfiguration config = CacheConfiguration.builder()
        .maxSize(10000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .evictionPolicy(EvictionPolicy.LRU)
        .serializer(new JacksonSerializer())
        .build();
```

---

#### 4.4 cache/redisson/ — Redisson 分布式缓存

##### 定位

基于 Redisson 的分布式缓存实现，适用于集群环境下的共享缓存。支持分布式锁、分布式集合、过期通知等能力。

```textmate
cn.jowen.framework.cache.cache.redisson
├─ RedissonCache                   # Redisson Cache 适配器
│  ├─ client: RedissonClient       # Redisson 客户端
│  ├─ name: String                 # 缓存名称
│  ├─ serializer: CacheSerializer  # 序列化器
│  ├─ get(String key, Class<T> type) -> T
│  ├─ put(String key, Object value, Duration ttl)
│  ├─ evict(String key)
│  ├─ clear()
│  └─ stats() -> CacheStats
└─ RedissonCacheManager             # Redisson CacheManager 实现
   ├─ client: RedissonClient        # Redisson 客户端
   ├─ create(String name, CacheConfiguration config) -> Cache
   └─ shutdown()
```

**使用示例**：

```textmate
RedissonClient redissonClient = Redisson.create(config);
CacheConfiguration config = CacheConfiguration.builder()
        .expireAfterWrite(Duration.ofHours(1))
        .serializer(new JacksonSerializer())
        .build();
Cache<String, User> remoteCache = new RedissonCache("remote-user", redissonClient, config);
```

---

#### 4.5 cache/multilevel/ — 多级缓存

##### 定位

组合本地缓存与分布式缓存，读取时先查 L1（Local），未命中再查 L2（Remote）并回填；写入时先写 L2 再写 L1，保证数据一致性。支持缓存穿透防护（空值缓存）、缓存雪崩防护（过期抖动）、缓存击穿防护（互斥锁）。

```textmate
cn.jowen.framework.cache.cache.multilevel
├─ MultilevelCache                 # 多级缓存实现
│  ├─ localCache: Cache
│  ├─ remoteCache: Cache
│  ├─ get(String key, Class<T> type) -> T
│  │  ├─ 1. 查 L1（本地缓存）→ 命中则返回
│  │  ├─ 2. 查 L2（分布式缓存）→ 命中则回填 L1 并返回
│  │  ├─ 3. 若开启 mutexLock → 加锁后回源查询
│  │  └─ 4. 写入 L2 → 写入 L1 → 返回
│  ├─ put(String key, Object value, Duration ttl)
│  │  ├─ 1. 写 L2（分布式缓存）
│  │  └─ 2. 写 L1（本地缓存）
│  ├─ evict(String key)
│  │  ├─ 1. 清除 L2
│  │  └─ 2. 清除 L1
│  └─ handleCacheMiss(String key, Function<String, T> loader) -> T
│     └─ 回源加载并回填多级缓存
├─ MultilevelCacheManager          # 多级缓存管理器
│  ├─ registerCache(String name, Cache local, Cache remote)
│  └─ getCache(String name) -> Cache
└─ CacheSyncListener               # 缓存同步监听器（跨节点通知）
   ├─ onCachePut(String key, Object value)
   └─ onCacheEvict(String key)
```

**使用示例**：

```textmate
// 读取：先查 local，miss 则查 remote 并回填 local
MultilevelCache multilevelCache = new MultilevelCache(localCache, remoteCache);
User user = multilevelCache.get("user:1001", User.class);

// 写入：先写 remote，再写 local
multilevelCache.put("user:1001", user, Duration.ofHours(1));

// 配置多级缓存管理器
MultilevelCacheManager manager = new MultilevelCacheManager();
manager.registerCache("user", localCache, remoteCache);
manager.registerCache("product", productLocalCache, productRemoteCache);
```

---

#### 4.6 event/ — 缓存事件

##### 定位

提供缓存操作的事件通知机制，支持命中、未命中、写入、清除等事件的监听。事件通过 SPI 机制异步分发，不影响主流程性能。

```textmate
cn.jowen.framework.cache.event
├─ CacheEvent                      # 缓存事件基类
│  ├─ cacheName: String
│  ├─ key: String
│  ├─ timestamp: long
│  └─ type: CacheEventType         # HIT/MISS/PUT/EVICT
├─ CacheHitEvent                   # 缓存命中事件
├─ CacheMissEvent                  # 缓存未命中事件
├─ CachePutEvent                   # 缓存写入事件
├─ CacheEvictEvent                 # 缓存清除事件
└─ CacheEventListener              # 事件监听器接口
   └─ void onEvent(CacheEvent event)
```

**使用示例**：

```textmate
// 注册事件监听器
cacheManager.addEventListener(event -> {
    if (event instanceof CacheMissEvent miss) {
        log.warn("Cache miss: cache={}, key={}", miss.getCacheName(), miss.getKey());
    }
});
```

---

#### 4.7 serializer/ — 缓存序列化

##### 定位

提供可插拔的序列化 SPI，默认使用 Jackson（tools.jackson，Jackson 3），支持 Kryo 等高性能序列化方案。序列化器通过 `SerializerFactory` 工厂类按需创建。

```textmate
cn.jowen.framework.cache.serializer
├─ CacheSerializer                 # 序列化接口
│  ├─ <T> byte[] serialize(T object)
│  ├─ <T> T deserialize(byte[] bytes, Class<T> type)
│  ├─ String getType()
│  └─ boolean supports(Class<?> type)
├─ JacksonSerializer               # Jackson 实现（默认，基于 tools.jackson）
│  ├─ ObjectMapper objectMapper   # Jackson 对象映射器
│  └─ 支持泛型类型反序列化
└─ SerializerFactory               # 序列化器工厂
   ├─ register(String type, CacheSerializer serializer)
   ├─ create(String type) -> CacheSerializer
   └─ getDefault() -> CacheSerializer
```

**使用示例**：

```textmate
// 使用默认 Jackson 序列化器
CacheSerializer serializer = SerializerFactory.getDefault();
byte[] bytes = serializer.serialize(user);
User deserialized = serializer.deserialize(bytes, User.class);

// 注册自定义序列化器
SerializerFactory.register("kryo", new KryoSerializer());
```

---

#### 4.8 support/ — 缓存键生成器与条件解析

##### 定位

提供缓存键生成策略和 SpEL 条件解析能力，支持自定义键生成逻辑和条件表达式求值。

```textmate
cn.jowen.framework.cache.support
├─ CacheKeyGenerator               # 缓存键生成器接口
│  └─ String generate(CacheOperationContext context)
├─ DefaultCacheKeyGenerator        # 默认键生成器
│  ├─ 基于方法参数名 + 参数值生成键
│  └─ 支持 SpEL 表达式求值
├─ ConditionEvaluator              # 条件表达式解析器抽象（零 Spring 依赖，仅定义契约）
│  ├─ boolean evaluate(String condition, CacheOperationContext context)
│  ├─ boolean evaluateUnless(String unless, CacheOperationContext context)
│  └─ protected abstract boolean doEvaluate(String expression, CacheOperationContext context)
│     框架默认实现位于 framework-boot-autoconfigure：`SpelConditionEvaluator`
└─ CacheOperationContext           # 缓存操作上下文
   ├─ beanName(): String
   ├─ method(): Method
   ├─ args(): Object[]
   ├─ target(): Object
   ├─ cacheName(): String
   ├─ key(): String
   ├─ condition(): String
   ├─ unless(): String
   ├─ isSync(): boolean
   ├─ keyGenerator(): String
   └─ listener(): String
```

**使用示例**：

```textmate
// 自定义缓存键生成器
@Component("tenantKeyGenerator")
public class TenantCacheKeyGenerator implements CacheKeyGenerator {
    @Override
    public String generate(CacheOperationContext context) {
        String tenantId = context.getArgs()[0].toString();
        String userId = context.getArgs()[1].toString();
        return tenantId + ":" + userId;
    }
}

// 使用自定义键生成器
@Cacheable(value = "user", keyGenerator = "tenantKeyGenerator")
public User getUser(String tenantId, Long userId) { ...}
```

---

#### 4.9 eviction/ — 缓存淘汰策略

##### 定位

定义缓存淘汰策略的抽象，支持 LRU 等策略，与底层缓存实现解耦。各淘汰策略可独立扩展，通过工厂按需创建。

```textmate
cn.jowen.framework.cache.eviction
├─ EvictionPolicy                  # 淘汰策略接口
│  ├─ String getType()
│  └─ boolean shouldEvict(EvictionContext context)
├─ LruEvictionPolicy               # LRU（最近最少使用）策略（默认）
└─ EvictionPolicyFactory           # 淘汰策略工厂
   ├─ register(String type, EvictionPolicy policy)
   └─ create(String type) -> EvictionPolicy
```

**使用示例**：

```textmate
CacheConfiguration config = CacheConfiguration.builder()
        .evictionPolicy(EvictionPolicy.LRU)
        .maxSize(5000)
        .build();
```

---

#### 4.10 lock/ — 缓存锁

##### 定位

基于 Redisson 的分布式锁实现，用于防止缓存击穿，确保同一时刻只有一个线程回源查询。支持可重入锁、公平锁和读写锁。

```textmate
cn.jowen.framework.cache.lock
├─ CacheLock                       # 缓存锁接口
│  ├─ boolean tryLock(Duration waitTime, Duration leaseTime)
│  ├─ void unlock()
│  └─ boolean isLocked()
├─ RedissonCacheLock               # Redisson 分布式锁实现
│  ├─ client: RedissonClient
│  ├─ lockName: String
│  ├─ boolean tryLock(Duration waitTime, Duration leaseTime)
│  └─ void unlock()
└─ CacheLockFactory                # 缓存锁工厂
   ├─ CacheLock getLock(String lockName)
   └─ void releaseLock(CacheLock lock)
```

**使用示例**：

```textmate
// 编程式使用缓存锁
CacheLockFactory lockFactory = new RedissonCacheLockFactory(redissonClient);
CacheLock lock = lockFactory.getLock("lock:user:1001");

try {
    if (lock.tryLock(Duration.ofSeconds(3), Duration.ofSeconds(30))) {
        try {
            User user = userRepository.findById(1001);
            cache.put("user:1001", user, Duration.ofHours(1));
        } finally {
            lock.unlock();
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

---

#### 4.11 config/ — 配置属性（纯 POJO）

##### 定位

缓存配置属性，不含 Spring 注解。`@ConfigurationProperties` 绑定由 `framework-boot-autoconfigure` 的 `CacheAutoConfiguration`（`@EnableConfigurationProperties`）完成。

```textmate
cn.jowen.framework.cache.config
└─ CacheProperties            # 配置属性（纯 POJO，前缀 framework.cache）
   ├─ enabled: boolean        # 是否启用缓存装配，缺省 true
   └─ metrics: boolean        # 是否暴露 Micrometer 指标，缺省 true
```

> 自动装配类（`CacheAutoConfiguration`）统一由 `framework-boot-autoconfigure` 模块的 `cache/` 子包承载。

---

## 五、核心类关系图

```text
┌────────────────────────────────────────────────────────────────────┐
│                      framework-cache                               │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    config (配置层)                           │  │
│  │  CacheProperties ─→ CacheAutoConfiguration (boot 装配层)     │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                             │ 创建                                 │
│                             ▼                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     api (核心抽象层)                         │  │
│  │  CacheManager / Cache / CacheConfiguration / CacheStats      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│          ▲                  ▲                  ▲                   │
│          │                  │                  │                   │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────┐         │
│  │ caffeine/     │  │ redisson/     │  │ multilevel/     │         │
│  │               │  │               │  │                 │         │
│  │ CaffeineCache │  │ RedissonCache │  │ MultilevelCache │         │
│  └───────────────┘  └───────────────┘  └─────────────────┘         │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     condition (契约层)                       │  │
│  │  ConditionEvaluator（SpEL 实现在 boot-autoconfigure）          │  │
│  └──────────────────────────────────────────────────────────────┘  │
│          ▲                                                        │
│          │ 由 AOP 切面注入                                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     support (工具层)                         │  │
│  │  CacheKeyGenerator / CacheOperationContext / CacheSerializer │  │
│  │  EvictionPolicy / CacheLock                                  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   annotation (注解层)                        │  │
│  │  @Cacheable / @CacheEvict / @CachePut / EnableCaching        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   framework-core                             │  │
│  │        (SPI / Event / Lifecycle / Assert)                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0 (零内部依赖)    support / event / eviction / lock
                      ▲
L1 (依赖 L0)        api (核心抽象层)
                      ▲
L2 (依赖 L0+L1)     cache/caffeine / cache/redisson / cache/multilevel / serializer
                      ▲
L3 (依赖 L0~L2)     annotation / config
```

---

## 七、外部依赖

```xml
<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>

    <!-- Caffeine 本地缓存（可选） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Redisson 分布式缓存（可选） -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Jackson 3 序列化（默认） -->
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Kryo 序列化（可选，高性能场景） -->
    <dependency>
        <groupId>com.esotericsoftware</groupId>
        <artifactId>kryo</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring AOP（可选，注解驱动需要） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-aop</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  cache:
    enabled: true                       # 是否启用缓存装配，缺省 true
    metrics: true                       # 是否暴露 Micrometer 指标，缺省 true

    # 默认缓存配置
    defaults:
      max-size: 10000
      expire-after-write: 30m
      serializer: jackson
      null-value-cache: true            # 缓存穿透防护：缓存空值
      jitter: 60s                       # 过期时间抖动（防雪崩）
      mutex-lock: true                  # 互斥锁（防击穿）
      eviction-policy: LRU              # 淘汰策略

    # 多级缓存配置
    multilevel:
      user:
        local:
          max-size: 1000
          expire-after-write: 5m
        remote:
          expire-after-write: 1h
        sync: true                      # 跨节点缓存同步
```

---

## 九、使用方式

#### 注解驱动

```textmate

@Service
public class UserService {

    // 缓存读取：命中则直接返回，未命中执行方法并缓存
    @Cacheable(value = "user", key = "#userId")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    // 缓存写入：方法执行后更新缓存
    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userRepository.update(user);
        return user;
    }

    // 缓存清除
    @CacheEvict(value = "user", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    // 条件缓存：仅当 userId > 0 时缓存
    @Cacheable(value = "user", key = "#userId", condition = "#userId > 0")
    public User getUserConditional(Long userId) {
        return userRepository.findById(userId);
    }
}
```

#### 编程式使用

```textmate

@Service
public class UserService {

    @Autowired
    private CacheManager cacheManager;

    public User getUser(Long userId) {
        Cache<String, User> cache = cacheManager.getCache("user");
        User user = cache.get("user:" + userId);
        if (user == null) {
            user = userRepository.findById(userId);
            if (user != null) {
                cache.put("user:" + userId, user, Duration.ofHours(1));
            }
        }
        return user;
    }
}
```

#### 多级缓存配置

```textmate

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager multilevelCacheManager(
            CacheManager localManager,
            CacheManager remoteManager) {
        MultilevelCacheManager manager = new MultilevelCacheManager();
        manager.registerCache("user",
                localManager.getCache("user-local"),
                remoteManager.getCache("user-remote")
        );
        manager.registerCache("product",
                localManager.getCache("product-local"),
                remoteManager.getCache("product-remote")
        );
        return manager;
    }
}
```

---

## 十、SPI 扩展点汇总

| 扩展点接口           | 所在包     | 用途                                            |
|:---------------------|:-----------|:------------------------------------------------|
| `CacheManager`       | api        | 缓存管理器扩展，自定义缓存创建与管理逻辑        |
| `Cache`              | api        | 缓存实例扩展，自定义缓存操作行为                |
| `CacheSerializer`    | serializer | 序列化扩展，支持自定义序列化方案（如 Protobuf） |
| `CacheKeyGenerator`  | support    | 缓存键生成策略扩展                              |
| `ConditionEvaluator` | condition  | 条件表达式解析器扩展（默认 SpEL 实现位于 framework-boot-autoconfigure） |
| `CacheEventListener` | event      | 缓存事件监听扩展                                |
| `EvictionPolicy`     | eviction   | 淘汰策略扩展                                    |
| `CacheLock`          | lock       | 缓存锁扩展，支持自定义分布式锁实现              |

---

## 十一、与整体框架的关系

```text
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                          │
│        @Cacheable / CacheManager / CacheLock                │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │   framework-cache               │
          │  ┌──────────────────────────┐   │
          │  │  annotation (AOP)        │   │
          │  ├──────────────────────────┤   │
          │  │ cache/multilevel         │   │
          │  ├──────────────────────────┤   │
          │  │ caffeine │ redisson      │   │
          │  ├──────────────────────────┤   │
          │  │ event │ serializer        │   │
          │  ├──────────────────────────┤   │
          │  │  support │ lock           │   │
          │  ├──────────────────────────┤   │
          │  │  eviction │ api           │   │
          │  └──────────────────────────┘   │
          └─────────────────────────────────┘
                           │
              ┌────────────▼─────────────┐
              │   framework-core          │
              │  SPI / Exception / Assert │
              └───────────────────────────┘
```

**Spring Boot 4.x 适配要点**：JacksonSerializer 基于 `tools.jackson`（Jackson 3）；统计上报 Micrometer 2.0；提供 `CacheRuntimeHints`（GraalVM 反射注册）。
