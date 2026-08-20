# framework-cache 模块架构设计

---

### 一、模块定位

framework-cache 是框架的 **多级缓存管理模块**，提供 **本地缓存（Caffeine）、分布式缓存（Redisson）以及多级缓存（Local + Remote
组合）** 等能力。该模块定义了统一的缓存抽象层，支持缓存注解（@Cacheable、@CacheEvict、@CachePut）、缓存事件监听、缓存序列化、缓存锁等能力。模块不依赖
Spring Boot，保持核心抽象的纯净性，同时提供完善的缓存穿透、雪崩、击穿防护机制。

本模块仅依赖 framework-core 提供的基础 SPI 与异常体系，缓存底层实现（如 Caffeine、Redisson）通过可选依赖引入，Spring Boot
集成通过独立的 starter 模块或 SPI 机制实现。

**核心价值**：

| 场景         | 没有本模块                                                   | 有本模块                                                             |
|--------------|--------------------------------------------------------------|----------------------------------------------------------------------|
| 多级缓存管理 | 业务方需自行维护本地与分布式缓存的一致性，容易出现数据不一致 | 提供统一的 Local + Remote 多级缓存抽象，自动处理读写顺序与数据回填   |
| 缓存异常防护 | 缺乏统一的穿透、雪崩、击穿防护机制，需各业务自行实现         | 内置空值缓存、过期抖动、互斥锁等防护策略，开箱即用                   |
| 缓存序列化   | 各业务模块序列化方式不统一，导致缓存数据不兼容               | 提供可插拔的序列化 SPI，默认 Jackson，支持 Kryo/Hessian 等高性能方案 |

**与核心模块的边界**：

| 模块                | 定位             | 特点                                                   |
|---------------------|------------------|--------------------------------------------------------|
| **framework-cache** | **多级缓存管理** | **统一缓存抽象、多级缓存、注解驱动、事件监听、缓存锁** |
| framework-core      | 基础设施         | SPI、异常、断言                                        |
| framework-data-*    | 数据访问         | 仓储、查询、事务                                       |

---

### 二、功能清单与依赖矩阵

| 功能                | 子包             | 核心依赖                        | 可选依赖   |
|---------------------|------------------|---------------------------------|------------|
| 缓存核心接口        | api              | —                               | —          |
| 缓存注解            | annotation       | framework-core                  | Spring AOP |
| Caffeine 本地缓存   | cache/caffeine   | Caffeine                        | —          |
| Redisson 分布式缓存 | cache/redisson   | Redisson                        | —          |
| 多级缓存            | cache/multilevel | cache/caffeine + cache/redisson | —          |
| 缓存事件            | event            | —                               | —          |
| 缓存序列化          | serializer       | Jackson                         | Kryo       |
| 缓存键生成器        | support          | —                               | —          |
| 缓存淘汰策略        | eviction         | —                               | —          |
| 缓存锁              | lock             | Redisson                        | —          |

---

### 三、整体包结构

```
framework-cache
└─ src/main/java/cn/jowen/framework/cache/
   ├─ api/                    # 缓存核心接口（CacheManager、Cache、CacheConfiguration）
   ├─ annotation/             # 缓存注解（@Cacheable、@CacheEvict、@CachePut、@EnableCaching）
   ├─ cache/                  # 缓存实现
   │  ├─ caffeine/            # Caffeine 本地缓存
   │  ├─ redisson/            # Redisson 分布式缓存
   │  └─ multilevel/          # 多级缓存（Local + Remote 组合）
   ├─ config/                 # 配置与自动装配
   ├─ event/                  # 缓存事件
   ├─ serializer/             # 序列化
   ├─ support/                # 缓存键生成器、条件解析
   ├─ eviction/               # 淘汰策略
   └─ lock/                   # 缓存锁
```

---

### 四、各子包详细设计

---

#### 4.1 api/ — 缓存核心接口

##### 定位

定义缓存模块的顶层抽象，包括 `CacheManager`、`Cache`、`CacheConfiguration` 等核心接口，所有缓存实现均基于此包进行适配。

```
cn.jowen.framework.cache.api
├─ CacheManager                    # 缓存管理器，负责创建和管理 Cache 实例
│  ├─ getCache(String name) -> Cache
│  ├─ getCache(String name, Class<T> type) -> Cache
│  ├─ createCache(String name, CacheConfiguration config)
│  └─ removeCache(String name)
├─ Cache                           # 缓存操作接口
│  ├─ get(String key, Class<T> type) -> T
│  ├─ put(String key, Object value)
│  ├─ put(String key, Object value, Duration ttl)
│  ├─ evict(String key)
│  ├─ clear()
│  ├─ containsKey(String key) -> boolean
│  ├─ size() -> long
│  └─ stats() -> CacheStats
├─ CacheConfiguration              # 缓存配置
│  ├─ maxSize: long               # 最大容量
│  ├─ expireAfterWrite: Duration  # 写入后过期时间
│  ├─ expireAfterAccess: Duration # 访问后过期时间
│  ├─ serializer: CacheSerializer # 序列化器
│  ├─ evictionPolicy: EvictionPolicy  # 淘汰策略
│  ├─ nullValueCache: boolean     # 是否缓存空值（防穿透）
│  ├─ jitter: Duration            # 过期时间抖动（防雪崩）
│  ├─ mutexLock: boolean          # 互斥锁（防击穿）
│  └─ builder() -> Builder
└─ NullValue                       # 空值标记，用于缓存穿透防护
   └─ static final NullValue INSTANCE
```

**使用示例**：

```textmate
CacheManager cacheManager = CacheManagerFactory.getDefault();
Cache userCache = cacheManager.getCache("user");
userCache.

put("user:1001",new User(1001, "Alice"));
User user = userCache.get("user:1001", User.class);
```

---

#### 4.2 annotation/ — 缓存注解

##### 定位

提供声明式缓存能力，通过 AOP 拦截方法调用，自动完成缓存的读取、写入和清除。

```
cn.jowen.framework.cache.annotation
├─ Cacheable                       # 缓存读取，命中则直接返回
│  ├─ value: String               # 缓存名称
│  ├─ key: String                 # 缓存键（支持 SpEL）
│  ├─ condition: String           # 条件表达式（SpEL）
│  ├─ unless: String              # 否定条件（SpEL）
│  ├─ sync: boolean               # 是否同步加载（默认 false）
│  └─ listener: String            # 事件监听器 Bean 名
├─ CacheEvict                      # 缓存清除
│  ├─ value: String               # 缓存名称
│  ├─ key: String                 # 缓存键（支持 SpEL）
│  ├─ allEntries: boolean         # 清除所有缓存
│  └─ beforeInvocation: boolean   # 是否在方法执行前清除
├─ CachePut                        # 缓存写入（不跳过方法执行）
│  ├─ value: String               # 缓存名称
│  ├─ key: String                 # 缓存键（支持 SpEL）
│  ├─ condition: String           # 条件表达式（SpEL）
│  └─ unless: String              # 否定条件（SpEL）
├─ EnableCaching                   # 启用缓存注解支持
│  └─ proxyTargetClass: boolean   # 是否使用 CGLIB 代理
└─ CacheAnnotationProcessor        # 注解处理器（AOP 切面）
   ├─ 解析 @Cacheable / @CacheEvict / @CachePut 注解
   ├─ 根据注解配置执行缓存操作
   └─ 支持条件表达式（SpEL）求值
```

**使用示例**：

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

---

#### 4.3 cache/caffeine/ — Caffeine 本地缓存

##### 定位

基于 Caffeine 的高性能本地缓存实现，适用于单机场景或作为多级缓存的 L1 层。支持基于内存大小的淘汰、基于时间的过期、基于引用的淘汰等策略。

```
cn.jowen.framework.cache.cache.caffeine
├─ CaffeineCache                   # Caffeine Cache 适配器
│  ├─ delegate: Cache<Object, Object>  # Caffeine Cache 实例
│  ├─ serializer: CacheSerializer  # 序列化器
│  ├─ get(String key, Class<T> type) -> T
│  ├─ put(String key, Object value, Duration ttl)
│  ├─ evict(String key)
│  ├─ clear()
│  └─ stats() -> CacheStats
└─ CaffeineCacheManager             # Caffeine CacheManager 实现
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
Cache localCache = new CaffeineCache("local-user", config);
```

---

#### 4.4 cache/redisson/ — Redisson 分布式缓存

##### 定位

基于 Redisson 的分布式缓存实现，适用于集群环境下的共享缓存。支持分布式锁、分布式集合、过期通知等能力。

```
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
   ├─ client: RedissonClient       # Redisson 客户端
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
Cache remoteCache = new RedissonCache("remote-user", redissonClient, config);
```

---

#### 4.5 cache/multilevel/ — 多级缓存

##### 定位

组合本地缓存与分布式缓存，读取时先查 L1（Local），未命中再查 L2（Remote）并回填；写入时先写 L2 再写
L1，保证数据一致性。支持缓存穿透防护（空值缓存）、缓存雪崩防护（过期抖动）、缓存击穿防护（互斥锁）。

```
cn.jowen.framework.cache.cache.multilevel
├─ MultilevelCache                 # 多级缓存实现
│  ├─ localCache: Cache           # L1 本地缓存
│  ├─ remoteCache: Cache          # L2 分布式缓存
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
multilevelCache.

put("user:1001",user, Duration.ofHours(1));

// 配置多级缓存管理器
MultilevelCacheManager manager = new MultilevelCacheManager();
manager.

registerCache("user",localCache, remoteCache);
manager.

registerCache("product",productLocalCache, productRemoteCache);
```

---

#### 4.6 event/ — 缓存事件

##### 定位

提供缓存操作的事件通知机制，支持命中、未命中、写入、清除等事件的监听。事件通过 SPI 机制异步分发，不影响主流程性能。

```
cn.jowen.framework.cache.event
├─ CacheEvent                      # 缓存事件基类
│  ├─ cacheName: String           # 缓存名称
│  ├─ key: String                 # 缓存键
│  ├─ timestamp: long             # 事件时间戳
│  └─ type: CacheEventType        # 事件类型
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
cacheManager.addEventListener(new CacheEventListener() {
    @Override
    public void onCacheMiss (CacheMissEvent event){
        log.warn("Cache miss: cache={}, key={}", event.getCacheName(), event.getKey());
    }

    @Override
    public void onCachePut (CachePutEvent event){
        log.info("Cache put: cache={}, key={}, ttl={}", event.getCacheName(), event.getKey(), event.getTtl());
    }
});

// 异步监听器（不阻塞主流程）
        cacheManager.

addAsyncEventListener(asyncListener);
```

---

#### 4.7 serializer/ — 缓存序列化

##### 定位

提供可插拔的序列化 SPI，默认使用 Jackson，支持 Kryo、Hessian 等高性能序列化方案。序列化器通过 `SerializerFactory`
工厂类按需创建，支持按缓存名称配置不同的序列化器。

```
cn.jowen.framework.cache.serializer
├─ CacheSerializer                 # 序列化接口
│  ├─ <T> byte[] serialize(T object)
│  ├─ <T> T deserialize(byte[] bytes, Class<T> type)
│  ├─ String getType()            # 序列化类型名称
│  └─ boolean supports(Class<?> type)  # 是否支持该类型
├─ JacksonSerializer               # Jackson 实现（默认）
│  ├─ ObjectMapper objectMapper   # Jackson 对象映射器
│  └─ 支持泛型类型反序列化
├─ KryoSerializer                  # Kryo 实现（高性能，可选）
│  ├─ Kryo kryo                   # Kryo 实例
│  └─ 线程安全：每个线程使用独立的 Kryo 实例
└─ SerializerFactory               # 序列化器工厂
   ├─ register(String type, CacheSerializer serializer)
   ├─ create(String type) -> CacheSerializer
   └─ getDefault() -> CacheSerializer
```

**使用示例**：

```textmate
// 使用 Jackson 序列化
CacheSerializer jacksonSerializer = SerializerFactory.create("jackson");
byte[] bytes = jacksonSerializer.serialize(user);
User deserialized = jacksonSerializer.deserialize(bytes, User.class);

// 使用 Kryo 序列化（高性能场景）
CacheConfiguration config = CacheConfiguration.builder()
        .serializer(SerializerFactory.create("kryo"))
        .build();

// 注册自定义序列化器
SerializerFactory.

register("hessian",new HessianSerializer());
```

---

#### 4.8 support/ — 缓存键生成器与条件解析

##### 定位

提供缓存键生成策略和 SpEL 条件解析能力，支持自定义键生成逻辑和条件表达式求值。

```
cn.jowen.framework.cache.support
├─ CacheKeyGenerator               # 缓存键生成器接口
│  └─ generate(CacheOperationContext context) -> String
├─ DefaultCacheKeyGenerator        # 默认键生成器
│  ├─ 基于方法参数名 + 参数值生成键
│  └─ 支持 SpEL 表达式求值
├─ ConditionEvaluator              # 条件表达式解析器
│  ├─ evaluate(String condition, CacheOperationContext context) -> boolean
│  └─ 基于 SpEL 表达式求值
└─ CacheOperationContext           # 缓存操作上下文
   ├─ methodName: String          # 方法名
   ├─ method: Method              # 方法对象
   ├─ args: Object[]              # 方法参数
   ├─ target: Object              # 目标对象
   └─ beanName: String            # Bean 名称
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

定义缓存淘汰策略的抽象，支持 LRU、LFU、TTL 等策略，与底层缓存实现解耦。各淘汰策略可独立扩展，通过 `EvictionPolicyFactory`
按需创建。

```
cn.jowen.framework.cache.eviction
├─ EvictionPolicy                  # 淘汰策略接口
│  ├─ String getType()            # 策略类型名称
│  └─ boolean shouldEvict(EvictionContext context)
├─ LruEvictionPolicy               # LRU（最近最少使用）策略
├─ LfuEvictionPolicy               # LFU（最近最不常用）策略
├─ TtlEvictionPolicy               # TTL（时间过期）策略
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

// 自定义淘汰策略
public class FifoEvictionPolicy implements EvictionPolicy {
    @Override
    public boolean shouldEvict(EvictionContext context) {
        // FIFO：先进先出淘汰
        return context.getAccessCount() == 0;
    }
}
```

---

#### 4.10 lock/ — 缓存锁

##### 定位

基于 Redisson 的分布式锁实现，用于防止缓存击穿，确保同一时刻只有一个线程回源查询。支持可重入锁、公平锁和读写锁。

```
cn.jowen.framework.cache.lock
├─ CacheLock                       # 缓存锁接口
│  ├─ tryLock(Duration waitTime, Duration leaseTime) -> boolean
│  ├─ unlock()
│  └─ isLocked() -> boolean
├─ RedissonCacheLock               # Redisson 分布式锁实现
│  ├─ client: RedissonClient       # Redisson 客户端
│  ├─ lockName: String             # 锁名称
│  ├─ tryLock(Duration waitTime, Duration leaseTime) -> boolean
│  └─ unlock()
└─ CacheLockFactory                # 缓存锁工厂
   ├─ getLock(String lockName) -> CacheLock
   └─ releaseLock(CacheLock lock)
```

**使用示例**：

```textmate
// 编程式使用缓存锁
CacheLockFactory lockFactory = new RedissonCacheLockFactory(redissonClient);
CacheLock lock = lockFactory.getLock("lock:user:1001");

try{
    if(lock.tryLock(3,Duration.ofSeconds(30))){
        try{
            // 回源查询并写入缓存
            User user = userRepository.findById(1001);
            cache.put("user:1001",user, Duration.ofHours(1));
        }finally{
            lock.unlock();
        }
    } else {
        throw new CacheLockException("Failed to acquire lock");
    }
} catch (InterruptedException e){
    Thread.currentThread().interrupt();
}
```

---

### 五、核心类关系图

```
┌────────────────────────────────────────────────────────────────────┐
│                      framework-cache                               │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    config (配置层)                           │  │
│  │  CacheAutoConfiguration ─→ CacheProperties                   │  │
│  │  ├─ CaffeineCacheManager                                     │  │
│  │  ├─ RedissonCacheManager                                     │  │
│  │  ─ MultilevelCacheManager                                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                             │ 创建                                 │
│                             ▼                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                     api (核心抽象层)                         │  │
│  │  CacheManager / Cache / CacheConfiguration                   │  │
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
│  │                     support (工具层)                         │  │
│  │  CacheKeyGenerator / CacheSerializer / CacheEventListener    │  │
│  │  EvictionPolicy / CacheLock                                  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   annotation (注解层)                        │  │
│  │  @Cacheable / @CacheEvict / @CachePut / EnableCaching        │  │
│  ───────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   framework-core                             │  │
│  │        (SPI / Event / Lifecycle / Assert)                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

### 六、分层依赖规则

```
L0 (零内部依赖)    support / event / eviction / lock
                      ▲
L1 (依赖 L0)        api (核心抽象层)
                      ▲
L2 (依赖 L0+L1)     cache/caffeine / cache/redisson / cache/multilevel / serializer
                      ▲
L3 (依赖 L0~L2)     annotation / config
```

---

### 七、外部依赖

```xml

<dependencies>
    <!-- 框架内依赖 -->
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>

    <!-- Caffeine 本地缓存 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Redisson 分布式缓存 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Jackson 序列化（默认） -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Kryo 序列化（可选） -->
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

### 八、配置属性

```yaml
jowen:
  cache:
    # 默认缓存配置
    defaults:
      expire-after-write: 30m
      max-size: 10000
      serializer: jackson
      null-value-cache: true        # 缓存穿透防护：缓存空值
      jitter: 60s                   # 过期时间抖动（防雪崩）
      mutex-lock: true              # 互斥锁（防击穿）
      eviction-policy: LRU          # 淘汰策略

    # 命名缓存配置
    caches:
      user:
        expire-after-write: 1h
        max-size: 5000
        serializer: kryo
      session:
        expire-after-write: 15m
        max-size: 20000
      product:
        expire-after-write: 2h
        max-size: 10000
        null-value-cache: true

    # 多级缓存配置
    multilevel:
      user:
        local:
          max-size: 1000
          expire-after-write: 5m
        remote:
          expire-after-write: 1h
        sync: true                  # 跨节点缓存同步

    # Redisson 配置
    redisson:
      address: redis://localhost:6379
      password: ${REDIS_PASSWORD}
      pool-size: 16
      connect-timeout: 10000
```

---

### 九、使用方式

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
        Cache cache = cacheManager.getCache("user");
        User user = cache.get("user:" + userId, User.class);
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

### 十、SPI 扩展点汇总

| 扩展点接口           | 所在包     | 用途                                            |
|----------------------|------------|-------------------------------------------------|
| `CacheManager`       | api        | 缓存管理器扩展，自定义缓存创建与管理逻辑        |
| `Cache`              | api        | 缓存实例扩展，自定义缓存操作行为                |
| `CacheSerializer`    | serializer | 序列化扩展，支持自定义序列化方案（如 Protobuf） |
| `CacheKeyGenerator`  | support    | 缓存键生成策略扩展                              |
| `ConditionEvaluator` | support    | 条件表达式解析器扩展                            |
| `CacheEventListener` | event      | 缓存事件监听扩展                                |
| `EvictionPolicy`     | eviction   | 淘汰策略扩展                                    |
| `CacheLock`          | lock       | 缓存锁扩展，支持自定义分布式锁实现              |

---

### 十一、与整体框架的关系

```
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                          │
│        @Cacheable / CacheManager / CacheLock                │
└─────────────────────────────────────────────────────────────┘
                       │
          ┌────────────▼────────────┐
          │   framework-cache       │
          │  ┌───────────────────┐  │
          │  │  annotation (AOP) │  │
          │  ├───────────────────   │
          │  │ cache/multilevel  │  │
          │  ├──────────────────    │
          │  │caffeine│ redisson │  │
          │  ├──────────────────    │
          │  │ event | serializer│  │
          │  ├───────────────────   │
          │  │  support | lock   │  │
          │  ├───────────────────   │
          │  │  eviction | api   │  │
          │  ────────────────────┘  │
          └─────────────────────────┘
                       │
         ┌─────────────▼─────────────┐
         │   framework-core          │
         │  SPI / Exception / Assert │
         └───────────────────────────┘
```
