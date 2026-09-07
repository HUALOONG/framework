package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 纯内存的 {@link RedisCommandExecutor} 测试夹具（Fake）。
 *
 * <p><b>用途</b>：为限流（T02）、装配（T03）、契约测试（T04）提供一个零外部依赖的
 * Redis 替身，使测试可以真实走通「脚本 → 计数器」链路，同时断言脚本入参。
 *
 * <p><b>为什么不用 Lua 解释器</b>：引入真实 Lua 解释器代价过高且无必要。本类的
 * {@code eval} 采用「<b>按脚本标识分派到预置 Java 实现</b>」的策略：
 * <ol>
 *   <li>先用 {@link #onScript(String, ScriptHandler)} 注册的处理器做精确匹配；</li>
 *   <li>再按注册键做「片段包含」匹配（便于只登记脚本常量里的特征行）；</li>
 *   <li>最后按内置标识 {@link #SCRIPT_FIXED_WINDOW} / {@link #SCRIPT_TOKEN_BUCKET}
 *       做包含匹配，落到内置的固定窗口 / 令牌桶实现；</li>
 *   <li>都不匹配则抛 {@link IllegalStateException}（提示"未预置该脚本"）——
 *       这样一旦限流器传了未预置的脚本，测试立刻失败，防止脚本常量与实现脱节。</li>
 * </ol>
 *
 * <p><b>已捕获调用</b>：{@link #capturedEvals()} 暴露历次 {@code eval} 的
 * script / keys / args，供调用方断言入参。
 *
 * <p><b>内置脚本约定</b>：
 * <ul>
 *   <li>{@link #SCRIPT_FIXED_WINDOW}：KEYS[1]=计数键；ARGV[1]=限流阈值 limit，
 *       ARGV[2]=窗口毫秒数。返回 1 表示放行、0 表示拒绝。</li>
 *   <li>{@link #SCRIPT_TOKEN_BUCKET}：KEYS[1]=桶键；ARGV[1]=容量 capacity，
 *       ARGV[2]=补充周期毫秒数，ARGV[3]=每周期补充令牌数（可省略，默认 1）。
 *       返回 1 表示取到令牌、0 表示桶空。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public class FakeRedisCommandExecutor implements RedisCommandExecutor {

    /** 内置脚本标识：固定窗口限流。 */
    public static final String SCRIPT_FIXED_WINDOW = "FIXED_WINDOW";

    /** 内置脚本标识：令牌桶限流。 */
    public static final String SCRIPT_TOKEN_BUCKET = "TOKEN_BUCKET";

    /** 脚本处理函数：把 Lua 语义等价实现为 Java。 */
    @FunctionalInterface
    public interface ScriptHandler {

        /**
         * 执行脚本对应的 Java 等价实现。
         *
         * @param keys KEYS 数组
         * @param args ARGV 数组
         * @return 脚本返回值，可为 {@code null}
         */
        @Nullable Object apply(List<String> keys, List<String> args);
    }

    /**
     * 一次 {@code eval} 调用的入参快照。
     *
     * @param script 脚本源码
     * @param keys   KEYS 数组
     * @param args   ARGV 数组
     */
    public record EvalCall(String script, List<String> keys, List<String> args) {
    }

    /** 带过期时间的内存值。 */
    private static final class Value {

        private final String data;
        private final long expireAtMillis;

        private Value(String data, long expireAtMillis) {
            this.data = data;
            this.expireAtMillis = expireAtMillis;
        }
    }

    private final Map<String, Value> store = new ConcurrentHashMap<>();
    private final Map<String, ScriptHandler> handlers = new ConcurrentHashMap<>();
    private final List<EvalCall> capturedEvals = Collections.synchronizedList(new ArrayList<>());
    private volatile LongSupplier clock = System::currentTimeMillis;

    /** 创建空的内存执行器，并预置 FIXED_WINDOW / TOKEN_BUCKET 的内置语义。 */
    public FakeRedisCommandExecutor() {
        handlers.put(SCRIPT_FIXED_WINDOW, this::fixedWindow);
        handlers.put(SCRIPT_TOKEN_BUCKET, this::tokenBucket);
    }

    /**
     * 注册（或覆盖）一段脚本的等价 Java 实现。
     *
     * @param script  脚本源码或可唯一识别该脚本的片段，不可为 {@code null} 或空白
     * @param handler 等价实现，不可为 {@code null}
     * @return 当前实例，便于链式注册
     */
    public FakeRedisCommandExecutor onScript(String script, ScriptHandler handler) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        handlers.put(script, handler);
        return this;
    }

    /**
     * 替换时间源，便于断言 TTL / 令牌补充等与时间相关的行为。
     *
     * @param clock 毫秒时间源，不可为 {@code null}
     * @return 当前实例
     */
    public FakeRedisCommandExecutor withClock(LongSupplier clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
        return this;
    }

    /**
     * 直接写入一个带 TTL 的键值，用于测试前置数据准备。
     *
     * @param key          键
     * @param value        值
     * @param expireMillis 过期毫秒数，{@code <= 0} 表示永不过期
     */
    public void put(String key, String value, long expireMillis) {
        long expireAt = expireMillis <= 0 ? Long.MAX_VALUE : currentMillis() + expireMillis;
        store.put(key, new Value(value, expireAt));
    }

    @Override
    public boolean setIfAbsent(String key, String value, long expireMillis) {
        if (liveValue(key) != null) {
            return false;
        }
        put(key, value, expireMillis);
        return true;
    }

    @Override
    public @Nullable String get(String key) {
        return liveValue(key);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean deleteIfMatch(String key, String value) {
        String current = liveValue(key);
        if (current == null || !current.equals(value)) {
            return false;
        }
        store.remove(key);
        return true;
    }

    @Override
    public boolean supportsScript() {
        return true;
    }

    @Override
    public @Nullable Object eval(String script, List<String> keys, List<String> args) {
        if (script == null) {
            throw new IllegalArgumentException("script must not be null");
        }
        capturedEvals.add(new EvalCall(script, List.copyOf(keys), List.copyOf(args)));
        ScriptHandler handler = resolveHandler(script);
        if (handler == null) {
            throw new IllegalStateException(
                    "FakeRedisCommandExecutor 未预置该脚本，请先通过 onScript(...) 注册等价实现："
                            + script);
        }
        return handler.apply(keys, args);
    }

    /**
     * 返回已捕获的 eval 调用快照（按调用顺序）。
     *
     * @return 不可变副本
     */
    public List<EvalCall> capturedEvals() {
        synchronized (capturedEvals) {
            return List.copyOf(capturedEvals);
        }
    }

    /**
     * 清空已捕获的 eval 调用记录。
     */
    public void clearCapturedEvals() {
        synchronized (capturedEvals) {
            capturedEvals.clear();
        }
    }

    /**
     * 返回最后一次 eval 调用的入参。
     *
     * @return 最后一次调用；从未调用过返回 {@code null}
     */
    public @Nullable EvalCall lastEval() {
        synchronized (capturedEvals) {
            return capturedEvals.isEmpty() ? null : capturedEvals.get(capturedEvals.size() - 1);
        }
    }

    /**
     * 返回当前未过期的键数量（过期键在访问时惰性清除）。
     *
     * @return 键数量
     */
    public int liveKeyCount() {
        store.keySet().forEach(this::liveValue);
        return store.size();
    }

    private @Nullable ScriptHandler resolveHandler(String script) {
        ScriptHandler exact = handlers.get(script);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, ScriptHandler> entry : handlers.entrySet()) {
            if (script.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private @Nullable String liveValue(String key) {
        Value value = store.get(key);
        if (value == null) {
            return null;
        }
        if (value.expireAtMillis != Long.MAX_VALUE && currentMillis() >= value.expireAtMillis) {
            store.remove(key);
            return null;
        }
        return value.data;
    }

    private long currentMillis() {
        return clock.getAsLong();
    }

    /**
     * 固定窗口：KEYS[1]=计数键，ARGV[1]=limit，ARGV[2]=窗口毫秒。
     *
     * @param keys KEYS 数组
     * @param args ARGV 数组
     * @return 1 放行 / 0 拒绝
     */
    private Object fixedWindow(List<String> keys, List<String> args) {
        String key = keys.get(0);
        long limit = Long.parseLong(args.get(0));
        long windowMillis = Long.parseLong(args.get(1));

        String current = liveValue(key);
        long count = current == null ? 0L : Long.parseLong(current);
        if (count >= limit) {
            return 0L;
        }
        put(key, String.valueOf(count + 1L), windowMillis);
        return 1L;
    }

    /**
     * 令牌桶：KEYS[1]=桶键，ARGV[1]=capacity，ARGV[2]=补充周期毫秒，ARGV[3]=每周期补充数（默认 1）。
     *
     * @param keys KEYS 数组
     * @param args ARGV 数组
     * @return 1 取到令牌 / 0 桶空
     */
    private Object tokenBucket(List<String> keys, List<String> args) {
        String key = keys.get(0);
        long capacity = Long.parseLong(args.get(0));
        long periodMillis = Long.parseLong(args.get(1));
        long refillTokens = args.size() > 2 ? Long.parseLong(args.get(2)) : 1L;

        long now = currentMillis();
        long tokens = capacity;
        long lastRefillMillis = now;
        String current = liveValue(key);
        if (current != null) {
            String[] parts = current.split("\\|", 2);
            tokens = Long.parseLong(parts[0]);
            lastRefillMillis = Long.parseLong(parts[1]);
            if (periodMillis > 0) {
                long periods = (now - lastRefillMillis) / periodMillis;
                if (periods > 0) {
                    tokens = Math.min(capacity, tokens + periods * refillTokens);
                    lastRefillMillis = lastRefillMillis + periods * periodMillis;
                }
            }
        }

        if (tokens <= 0L) {
            put(key, tokens + "|" + lastRefillMillis, 0L);
            return 0L;
        }
        put(key, (tokens - 1L) + "|" + lastRefillMillis, 0L);
        return 1L;
    }
}
