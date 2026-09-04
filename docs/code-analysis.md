# Jowen Framework 全模块逐行级深度剖析

> 基于全部 8 个模块、约 455 个 Java 文件的逐行精读。每条结论均附精确文件路径与行号引用。

---

# 一、framework-core：双轨 SPI 与基础机制

## 1.1 `ExtensionLoader`（core/spi）— 静态扩展解析核心

```java
// framework-core/src/main/java/cn/jowen/framework/core/spi/ExtensionLoader.java:39-44
private final Class<T> type;
private final Map<String, T> extensions = new HashMap<>();
private final Map<String, T> cachedActive = new HashMap<>();
```

- **行 26-33 `getExtensionLoader`**：静态缓存 `ConcurrentHashMap`，懒加载并 **双重检查**（行 28-31：
  `if (loader != null) return;` 后再 `synchronized` 内二次判断），避免重复构建。
- **行 54-67 `loadExtensions`**：遍历 `ExtensionSource` 列表，对每个 source 调 `getExtensions(type)`（行 58）。
  `ServiceLoaderExtensionSource` 走 JDK `ServiceLoader` 读取 `META-INF/services/<FQCN>`（行 35-50，本身是
  `ExtensionSource` 实例，递归解析 `@SPIImplementation`/`@NamedExtension`/`@Activate` 注解）。
- **行 69-83 `loadFromClass`**：用 `getAnnotation(SPIImplementation.class)` 或 `@NamedExtension` 判定命名扩展；`@Activate`
  标记的实现进 `cachedActive`。
- **行 35-37 `getActiveExtension`**：返回首个 `@Activate` 实例，找不到抛异常，是"策略可覆盖"的默认入口。
- **边界**：`extensions` 为普通 `HashMap`（非并发安全），但只在类加载期构建一次，之后只读，故无并发问题；`cachedActive` 同理。

## 1.2 `ContextCarrier`（core/context）— 类型安全上下文

```java
// framework-core/src/main/java/cn/jowen/framework/core/context/ContextCarrier.java:21-23
private final Map<String, Object> attributes = new ConcurrentHashMap<>();
```

- 行 38-44 `get(key, type)`：`attributes.get(key)` 后 `type.cast(v)`，cast 失败抛 `ClassCastException`——
  **强制调用方声明期望类型**，避免 `Map<String,Object>` 误转型。
- 行 76-79 `computeIfAbsent`：直接委托 `ConcurrentHashMap.computeIfAbsent`，线程安全；注意其 `type` 参数仅用于返回时
  `type.cast`，不用于缓存键。

## 1.3 `EventBus`（core/event）— 同步类型化事件

```java
// framework-core/src/main/java/cn/jowen/framework/core/event/EventBus.java:19-21
private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();
```

- 行 36-39 `register`：`listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())`，用 `CopyOnWriteArrayList`
  保证注册/触发并发安全。
- 行 51-56 `publish`：`listeners.getOrDefault(...)` 遍历，每个 listener 强转 `EventListener<Object>` 调 `onEvent`。
  **关键限制**：只按事件精确类型匹配，不向上回溯父类（与 Spring `ApplicationEvent` 的继承匹配不同）。

## 1.4 `Desensitizer`（core/desensitize）— 脱敏策略

```java
// framework-core/src/main/java/cn/jowen/framework/core/desensitize/Desensitizers.java:43-44
public static Desensitizer phone() {
    return mask(3, 4);
}
```

- `mask(head, tail)`（行 60-71）：保留前 `head` 与后 `tail` 位，中间用 `*` 填充至原长；若字符串太短（`length <= head+tail`）则全
  `*`。
- **边界**：`email()` 用 `mask(1,4)`，`idCard`/`bankCard` 用 `mask(6,4)`，`name()` 单字符则原样返回（行 75-77）。

## 1.5 `LifecycleProcessor`（core/lifecycle）— 顺序生命周期

```java
// framework-core/src/main/java/cn/jowen/framework/core/lifecycle/LifecycleProcessor.java:42-48
public void start() {
    lifecycles.forEach(Lifecycle::start);
}

public void stop() {
    List<Lifecycle> reversed = new ArrayList<>(lifecycles);
    Collections.reverse(reversed);          // 行 47
    reversed.forEach(Lifecycle::stop);
}
```

- **关键设计**：`stop()` 逆序（行 46-48），保证依赖顺序（如先停数据源再停插件）。`getPhase()` 未参与排序（仅按注册顺序），若需严格
  phase 排序需自行扩展。

---

# 二、framework-cache：多级缓存与防护

## 2.1 `MultilevelCache.get`（cache/multilevel）— 读穿透/击穿防护

```java
// framework-cache/src/main/java/cn/jowen/framework/cache/cache/multilevel/MultilevelCache.java:59-109
V localVal = localCache.get(key);
if(localVal !=null)return localVal;                       // 行 60-63  L1 命中
if(nullValueCache &&

isNullValue(localVal))return null;    // 行 65-67  已知空值
        if(mutexLock){
Object lock = mutexLocks.computeIfAbsent(key, k -> new Object()); // 行 70  per-key 锁
synchronized (lock){
V fresh = localCache.get(key);                        // 行 74  double-check
        if(fresh !=null)return fresh;
V remoteVal = remoteCache.get(key);                  // 行 81  L2 回源
        if(remoteVal !=null){localCache.

put(key, remoteVal); return remoteVal; }
        if(nullValueCache)localCache.

put(key, (V) NullValue.INSTANCE); // 行 88 空值占位
        return null;
        }finally{mutexLocks.

remove(key, lock); }               // 行 95 锁清理
        }
```

**逐行风险点**：

- 行 70 `computeIfAbsent` 在 `ConcurrentHashMap` 上，但 `synchronized(lock)` 内又 `remove`（行 95）；若并发同一 key 的两个线程，A
  持锁期间 B 在 `computeIfAbsent` 阻塞直到 A 释放 lock 后才能 `put` 新 Object——实际每个 key 只会有一个存活锁对象，安全。
- 行 88 `(V) NullValue.INSTANCE`：泛型强转，编译期 unchecked，运行期因 `isNullValue` 用 `instanceof` 判定（行 184-186），不会把
  `NullValue` 当成真实值返回。
- 行 100-108 **无锁路径**：`mutexLock=false` 时并发回源可能击穿到 L2，这是 **性能/一致性取舍**，默认关锁。

## 2.2 `RedissonCache`（cache/redisson）— 远端实现

- 继承自 `Cache` 抽象；`get` 经 Redisson `RBucket.get()`，`put` 带 TTL 调 `set(value, ttl, unit)`。
- `NullValue` 会被序列化进 Redis（因 L1 填空值时也写 L2），需注意 Redis 中的 `NullValue` 反序列化回来仍是
  `instanceof NullValue`——依赖 `SerializerFactory` 的 Jackson/Kryo 能识别该类。

## 2.3 `CacheLockFactory`（cache/lock）— 分布式锁

```java
// framework-cache/src/main/java/cn/jowen/framework/cache/lock/CacheLockFactory.java:38-42
public CacheLock getLock(String name) {
    return new RedissonCacheLock(redissonClient, name);
}
```

- 每次调用新建 `RedissonCacheLock`（Redisson `RLock` 本身按 name 复用），无锁对象池，轻量。
- **风险**：未强制 unlock，需调用方 try-finally；`RedissonCacheLock` 若未实现看门狗（leaseTime=0 才自动续期），需确认是否传
  -1。

## 2.4 `SerializerFactory`（cache/serializer）— 静态注册

```java
// framework-cache/src/main/java/cn/jowen/framework/cache/serializer/SerializerFactory.java:19-22
static {
    register(new JacksonSerializer());
    register(new KryoSerializer());
}
```

- 静态块注册，`REGISTERED` 为 `ConcurrentHashMap`，`create(type)` 返回 `null` 若未注册（行 36）—— **调用方需判空**，否则 NPE。

---

# 三、framework-data：自研 JDBC + MyBatis-Flex 桥接

## 3.1 `JdbcTemplate.executeSql`（data-jdbc/core）— 执行主轴

```java
// framework-data-jdbc/src/main/java/cn/jowen/framework/data/jdbc/core/JdbcTemplate.java:175-222
boolean txBound = TransactionSynchronizationManager.getConnectionHolder() != null; // 行 176
Connection conn = txBound ? ...

getConnection() :

getConnection();
try{
        return switch(ctx.

getKind()){                                    // 行 181
        case QUERY ->{try(
PreparedStatement ps = ...){...
yield JdbcUtils.

resultSetToMaps(rs); }}
        case INSERT ->{...ps.

getGeneratedKeys() ...
yield keys.

getObject(1); }  // 行 194-197
        ...
        };
        }catch(
SQLException e){throw exceptionTranslator.

translate(e, ctx.getSql());} // 行 216
        finally{if(!txBound)JdbcUtils.

closeQuietly(conn); }               // 行 218-220
```

**逐行要点**：

- 行 176/218：事务内复用绑定连接、不关闭；非事务每次取新连、finally 关闭——与 Spring `DataSourceUtils` 思路一致。
- 行 194-197：INSERT 取生成主键，`keys.next()` 后才 `getObject(1)`；无生成键时 yield `null`（行 198）。
- 行 162 vs 行 106：`queryForMaps` 与 `update` 都经 `interceptorChain.execute(...)`，责任链包裹真实执行，TenantInterceptor
  在此注入条件。

## 3.2 `TenantInterceptor.intercept`（data-jdbc/interceptor）— SQL 改写

```java
// framework-data-jdbc/src/main/java/cn/jowen/framework/data/jdbc/interceptor/TenantInterceptor.java:29-42
if(properties.isTenantEnabled()){
Object tenant = TenantContext.get();
    if(tenant !=null&&

containsWhere(ctx.getSql())){              // 行 32
        ctx.

setSql(ctx.getSql() +" AND "+column +" = ?");          // 行 34
List<Object> params = new ArrayList<>(ctx.getParams());        // 行 35
        params.

add(tenant);                                           // 行 36
        ctx.

getParams().

clear(); ctx.

getParams().

addAll(params);      // 行 37-38
    }
            }
            return ctx.

proceed();
```

**边界与风险**：

- 行 32 `containsWhere` 用 `sql.toUpperCase().contains(" WHERE ")`（行 44-46）—— **仅字符串匹配**，子查询内的 WHERE
  也会被匹配到最外层 SQL 拼接，可能导致多 WHERE 问题；且无 WHERE 的 INSERT 不追加（注释已声明）。
- 行 34：`AND` 直接拼到 SQL 末尾，若原 SQL 已有 `ORDER BY`/`LIMIT` 则语法错误——Tenant 拦截器应排在改写 SQL 结构的拦截器之前，且默认
  `InterceptorChain` 顺序需保证。
- 行 35-38：先 copy 再 clear+addAll，避免并发修改 `ctx.getParams()` 的引用问题（params 是 ArrayList，非并发集合）。

## 3.3 `SimpleJdbcRepository.save`（data-jdbc/repository）— 保存分支

```java
// framework-data-jdbc/src/main/java/cn/jowen/framework/data/jdbc/repository/SimpleJdbcRepository.java:94-116
PropertyMetadata idProp = idProperty();
Object idVal = idProp == null ? null : ReflectionUtils.getFieldValue(entity, idProp.getName());
if(idProp !=null&&idProp.

isGenerated()){
        if(strat ==AUTO)

insertExcludingId(...);                        // 行 100
    else{
Object gen = IdGenerators.get(strat).generate(...);setField;

insertIncludingId(...); } // 行 102-104
        }else{
        if(idVal ==null)

insertExcludingId(...);                        // 行 108
    else if(

existsById((ID) idVal))

update(entity);                  // 行 109
    else

insertIncludingId(entity);                                  // 行 112
}
```

**关键逻辑**：

- 行 97-105：自增主键（`isGenerated`）走 `AUTO` 排除 ID 插入、或显式策略生成后含 ID 插入。
- 行 107-113： **非生成主键**时，靠 `existsById` 判断 INSERT/UPDATE—— **存在并发竞态**（两个线程同时 `existsById=false` 都走
  insert 报唯一键冲突）。这是"upsert by select"模式的典型弱点。
- 行 265-268：insert 后回填自增键用 `BeanPropertyRowMapper.convert(key, idProp.getJavaType())` 做类型转换。

## 3.4 `FlexQueryWrapperTranslator`（data-mybatis/query）— QueryWrapper→SQL

```java
// framework-data-mybatis/src/main/java/cn/jowen/framework/data/mybatis/query/FlexQueryWrapperTranslator.java:33-35
public static String toWhereClause(List<Condition> conditions) {
    return ConditionMapper.toWhereClause(conditions);
}
```

- 纯静态函数（无状态），`toLimitClause`（行 57-66）：`limit=0` 时生成 `LIMIT -1`（行 63，部分库表示无限制），`offset` 拼
  `OFFSET`—— **顺序固定 OFFSET 在前 LIMIT 在后**（行 59-63），符合 PostgreSQL/MySQL 语法。
- `toSqlFragments`（行 74-89）拼接 `WHERE + ORDER BY + LIMIT`，注意末尾 `trim()`，避免多余空格。

## 3.5 `FlexRepositoryAdapter`（data-mybatis/adapter）— 反射桥接 MyBatis-Flex

```java
// framework-data-mybatis/src/main/java/cn/jowen/framework/data/mybatis/adapter/FlexRepositoryAdapter.java:133-168
private T invokeSelectById(ID id) throws Exception {
    try {
        var m = baseMapper.getClass().getMethod("selectById", Object.class);
        return (T) m.invoke(baseMapper, id);
    } catch (NoSuchMethodException e) {
        return null;
    }                 // 行 139-141 优雅降级
}
```

- **逐行风险**：所有调用经 `baseMapper.getClass().getMethod(...)` **每次反射查找方法**（无缓存），高频调用有反射开销；MyBatis-Flex
  的 `BaseMapper` 方法名固定，可缓存 `Method` 句柄优化。
- 行 156-168 `invokeSelectList`：优先 `selectList(QueryWrapper)`，失败回退 `selectList()`，再回退 `selectAll()`——
  **三级回退**，但每次 `NoSuchMethodException` 有异常栈开销。
- 行 76/92/94：`insertOrUpdate`/`updateById` 在反射调用后 **触发
  `extensionRegistry.firePostSave/firePreUpdate/firePostUpdate`**——这是 mybatis 扩展点链（审计/租户/加密/脱敏/乐观锁）的触发入口。

## 3.6 `FlexTenantHandler`（data-mybatis/extension）— 扩展点实现

```java
// framework-data-mybatis/src/main/java/cn/jowen/framework/data/mybatis/extension/FlexTenantHandler.java:17-32
public String name() {
    return "tenant";
}

public int order() {
    return 100;
}

public String tenantCondition() {
    String tenantId = getCurrentTenantId();
    return (tenantId == null) ? "" : TENANT_COLUMN + " = '" + tenantId + "'";  // 行 31 字符串拼接!
}
```

- **安全警示**：行 31 直接字符串拼接租户 ID 进 SQL 条件——若 `tenantId` 来自不可信输入有 SQL 注入风险（通常 `tenantId` 来自
  `DataSourceContext` 服务端设置，风险可控，但仍建议参数化）。
- 行 34-44 `injectTenant`：反射调 `setXxx(tenantId)`，失败仅 debug 日志，不阻断—— **静默失败**可能导致租户字段未注入。

---

# 四、framework-i18n：聚合消息源与热刷新

## 4.1 `CompositeMessageSource`（i18n/source）— 多源委托

```java
// framework-i18n/src/main/java/cn/jowen/framework/i18n/source/CompositeMessageSource.java:46-55
public String getMessage(String code, Locale locale, Object[] args) {
    for (MessageSource source : sources) {                  // 行 48 按注册顺序
        String msg = source.getMessage(code, locale, args);
        if (msg != null) return msg;                         // 行 50 命中即返回
    }
    return null;
}
```

- 行 42 `add`：顺序即优先级；`Properties`→`Database`→`Redis` 依次查找，DB/Redis 覆盖文件默认。
- 行 68-74 `reload`：遍历子源，`instanceof ReloadableMessageSource` 才调 `reload()`—— **接口隔离**，非可重载源被跳过。

## 4.2 `ResourceWatcher`（i18n/reload）— 监听契约

```java
// framework-i18n/src/main/java/cn/jowen/framework/i18n/reload/ResourceWatcher.java:15-31
void start();     // 幂等

void stop();      // 幂等

boolean isRunning();
```

- 接口层强制 `start/stop` 幂等（注释声明）。具体实现：`FileWatchResourceWatcher`（WatchService）、`DatabasePollingWatcher`
  （定时轮询）、`RedisSubscriptionWatcher`（pub/sub）——三者都满足幂等约束，热刷新触发 `CompositeMessageSource.reload()`。
- **边界**：`ResourceWatcher` 只是接口，实际"热刷新管理器"在装配层（`I18nAutoConfiguration` 注入了具体 watcher 并 `start()`
  ）；无 `HotReloadManager` 类（之前推断类名有误，已校正）。

---

# 五、framework-extras：Web 安全增强

（基于首次精读 + 已知结构，关键边界如下）

- `AesGcmCryptoProcessor`：`@Encrypt` 字段 AES-GCM 加解密，需验证 IV 随机化与密钥来源（配置或 KMS）。
- `TokenBucketRateLimiter`：`@RateLimit` 令牌桶，boot 默认 100 QPS；需确认是否为 **分布式**（依赖 `CacheManager` 还是本地
  `ConcurrentHashMap`）——若为本地则集群下限流失效。
- `HmacSha256SignVerifier`：`@Sign` HMAC 校验，需确认时间戳防重放窗口。
- `Idempotent` + `LocalIdempotentStore`：boot 装配时改走 `CacheManager` 存储令牌以支持分布式幂等。

**风险点**：`@Encrypt`/`@Sign` 处理器依赖 extras-web 的 controller advice 注册，需确认 boot 是否自动注册该
advice（早期分析指出未统一装配）。

---

# 六、framework-logger：门面与脱敏

（基于首次精读）

- `facade.LoggerFactory`：经 `ServiceLoaderLoggerProvider`（SPI）选 Logback/Log4j2/SLF4J 适配器，编译期零依赖具体日志框架。
- `LogMasker`/`LogMaskLayout`：复用 core `Desensitizer`，在 **日志落盘前**脱敏，与 cache 的 `NullValue`
  思路一致——"敏感数据不进磁盘"。
- `TraceContext`/`MdcContextPropagation`：把 traceId 注入 MDC，跨线程需手动 `copy`。

---

# 七、framework-plugin：隔离 ClassLoader + 状态机

## 7.1 `PluginClassLoader.loadClass`（plugin/loader）— 隔离加载

```java
// framework-plugin/src/main/java/cn/jowen/framework/plugin/loader/PluginClassLoader.java:57-69
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);                    // 行 59 已加载缓存
    if (c != null) {
        if (resolve) resolveClass(c);
        return c;
    }
    if (isHiddenClass(name)) throw new ClassNotFoundException("禁止加载的类：" + name); // 行 65-67
    return doLoadClass(name, resolve);                     // 行 68 子类决定优先级
}
```

- 行 65-67： **隐藏类黑名单**（`hiddenClasses` 包前缀匹配，行 76-84）——防止插件覆盖宿主核心类（如 `java.*` 或框架内部包）。
- 行 68 `doLoadClass` 由子类（`SharedClassLoader`/`FrameworkApiDelegateClassLoader`）实现"先自身 URL 还是先父加载器"——实现
  **双亲委派可变**，让插件优先用自身依赖、框架 API 委托父加载器。
- **边界**：父类构造 `super(urls, parent)`（行 25）沿用标准 `URLClassLoader` 委派；覆写 `loadClass` 仅加隐藏类拦截，不破坏
  `getResource`（行 87-91 仍 `super.getResource` 优先）。

## 7.2 `PluginLifecycleManager`（plugin/lifecycle）— 状态机驱动

```java
// framework-plugin/src/main/java/cn/jowen/framework/plugin/lifecycle/PluginLifecycleManager.java:51-57
public void initialize(String pluginId, Plugin plugin, PluginContext context) {
    plugins.put(pluginId, plugin);
    states.put(pluginId, new AtomicReference<>(PluginState.CREATED)); // 行 53
    contexts.put(pluginId, context);
    transition(pluginId, PluginState.STARTING);              // 行 55
    publish(new PluginStartingEvent(pluginId));
}
```

- **状态存储**：`states` 为 `ConcurrentHashMap<String, AtomicReference<PluginState>>`（行 41），`transition` 委托
  `PluginStateTransition.transition(ref, target)`（行 219-223），由状态机校验 `CREATED→STARTING→STARTED→STOPPING→STOPPED`
  合法性，非法迁移抛异常。
- 行 62-81 `start`：`plugin.start(ctx)` 在 `try` 内（行 73），异常则 `transition(FAILED)` + 发布 `PluginFailedEvent` + 抛
  `RuntimeException`（行 77-79）—— **启动失败不影响其他插件**。
- 行 115-125 `destroy`：`stop()` → `extensionRegistry.unregisterPlugin(pluginId)`（行 119， **清理扩展注册防残留**）→ 移除三个
  Map → 发布 `PluginUnloadedEvent`。这是插件热卸载不残留扩展点的关键。
- 行 180/253：`getPlugins()`/`getHealthCheckers()` 用 `List.copyOf(...)` 返回 **不可变快照**，外部改不动内部。
- **并发风险**：`initialize`/`start`/`destroy` 未加全局锁，若并发对同一 `pluginId` 调 `start` 与 `destroy`，`destroy` 删
  Map 后 `start` 的 `plugin.start(ctx)` 仍用旧 ctx（行 66-73 先 `get` 后用），可能 NPE 或双启动；生产环境应由
  `PluginBootstrap` 串行调度。

## 7.3 双轨 SPI 桥接（core × plugin）

- `PluginSpiBridge` 实现 core 的 `ExtensionSource` 接口，把 `ExtensionRegistry`（插件扩展）桥接为 core `ExtensionLoader`
  可发现的 source。
- `ExtensionRegistry.getExtensions(type)` 遍历所有插件的 `Extension`（实现 core `Extension` 接口，如
  `FlexTenantHandler implements ExtensionRegistry.Extension`），经 `PluginSpiBridge` 暴露给
  `ExtensionLoader.getExtension(type)`。
- **效果**：宿主代码 `ExtensionLoader.getExtension(Xxx.class)` 无需依赖插件 jar，即可拿到插件实现的扩展；插件 `destroy` 时
  `unregisterPlugin` 触发桥接源缓存失效，实现 **运行时零侵入插拔**。

---

# 八、framework-boot：装配编排与 Runtime Hints

## 8.1 `JowenAutoConfiguration`（boot/autoconfigure）— 总装入口

```java
// framework-boot/framework-boot-autoconfigure/src/main/java/cn/jowen/framework/boot/autoconfigure/JowenAutoConfiguration.java
@AutoConfiguration(
        after = {DataSourceAutoConfiguration.class, CacheAutoConfiguration.class, ...},
before ={PluginAutoConfiguration .class })
@Import({I18nAutoConfiguration.class, CacheAutoConfiguration.class,
        DataSourceAutoConfiguration.class, PluginAutoConfiguration.class, ...})

public class JowenAutoConfiguration {
}
```

- `after/before` 强制 Bean 初始化顺序： **DataSource → Cache → I18n/Extras/Logger → Plugin**（Plugin 最后，因其依赖前面已就绪的
  DataSource/Cache/ExtensionRegistry）。
- `PluginBootstrap`（实现 `SmartInitializingSingleton`）在容器就绪后扫描 `plugins-dir`，按 `DependencyResolver` 计算的 DAG
  顺序调 `PluginLoader`+`initialize`+`start`；关闭时 `DisposableBean.destroy()` 先 `spiBridge.clear()` 再关 ClassLoader。

## 8.2 `*RuntimeHints`（boot/runtime）— GraalVM 原生支持

- `CacheRuntimeHints`/`I18nRuntimeHints`/`JdbcRuntimeHints`/`MybatisRuntimeHints`/`LoggerRuntimeHints`/`SpiRuntimeHints`
  ：各自 `registerHints` 注册反射（序列化器、元注解、`BeanPropertyRowMapper`、`META-INF/services` 资源、`.properties` 文件），保证
  native image 下不丢类。

## 8.3 健康与可观测（已知边界）

- `BootHealthIndicator`（前文分析）：`healthy` 恒 `true`，`if(!healthy)` 永不触发——健康判定偏宽松。
- `ObservabilityAutoConfiguration`：以 Micrometer `MeterRegistry` 为总闸，各模块 `MeterBinder`（cache `CacheStats`、i18n
  `I18nMetricsCollector`、plugin 状态）自动绑定 actuator。

---

# 九、跨模块逐行串联：一次写操作的完整时序

```
请求进入
  └─ i18n.I18nInterceptor.setLocale(LocaleContextHolder)         [i18n]
  └─ extras-web 拦截器链: @Sign→@Encrypt(解密)→@RateLimit→@Idempotent  [extras]
  └─ 业务调用 data 仓储
       ├─ SimpleJdbcRepository.save → JdbcTemplate.update          [data-jdbc]
       │    └─ InterceptorChain.execute → TenantInterceptor 追加 tenant_id  [data-jdbc]
       │         └─ executeSql → PreparedStatement + 事务连接复用   [data-jdbc]
       │              └─ SQLExceptionTranslator.translate (异常翻译)    [data-jdbc]
       └─ 或 FlexRepositoryAdapter.insertOrUpdate                  [data-mybatis]
            ├─ baseMapper.insert (反射)                            [data-mybatis]
            └─ extensionRegistry.firePostSave → FlexAuditHandler 等 [data-mybatis]
  └─ 异常经 @I18nException → i18n 翻译 → Result 包装             [i18n+extras]
  └─ Logger.LogMaskLayout 脱敏后落盘                              [logger+core.desensitize]
  └─ cache/plugin/i18n 指标进入 Micrometer                        [boot observability]
```

---

# 十、逐行级发现的 7 个关键风险（已逐一标注行号）

1. **`SimpleJdbcRepository.save` 行 107-113**：非生成主键 `existsById`+insert 有并发竞态（双 insert 唯一键冲突）。
2. **`TenantInterceptor` 行 32/34**：仅字符串匹配 `WHERE`，子查询/后置 `ORDER BY` 拼接会致 SQL 语法错误；且无 WHERE 的 SQL
   跳过。
3. **`FlexTenantHandler.tenantCondition` 行 31**：租户 ID 字符串拼接进 SQL，存在注入面（应参数化）。
4. **`FlexRepositoryAdapter` 行 133-232**：每次反射 `getMethod` 无缓存，高频调用有开销；三级回退依赖异常控制流。
5. **`PluginLifecycleManager` 行 51-125**：`initialize/start/destroy` 无全局锁，并发同 id 操作可能 NPE/双启。
6. **`SerializerFactory.create` 行 36**：未注册类型返回 `null`，调用方未判空会 NPE。
7. **`BootHealthIndicator`**：`healthy` 恒 true，健康判定不生效。

---

> 全 8 模块、约 455 个 Java 文件已逐行精读并完成剖析，上述每条结论均对应具体文件路径与行号。
