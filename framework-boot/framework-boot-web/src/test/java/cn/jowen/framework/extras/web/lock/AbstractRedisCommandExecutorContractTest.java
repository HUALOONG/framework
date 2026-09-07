package cn.jowen.framework.extras.web.lock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisCommandExecutor} 契约测试套件（抽象基类，方案 A）。
 *
 * <p><b>用途</b>：业务方（或未来的 Jedis / Lettuce / StringRedisTemplate 适配器）实现本 SPI 后，
 * 继承本类并实现 {@link #createExecutor()} 即可一键自证「我的实现符合框架契约」，尤其是新增的
 * 脚本执行语义。所有契约项由 {@link #verify()} 统一执行并<b>收集失败</b>（而非 fail-fast），
 * 便于一次看到全部不符点。</p>
 *
 * <p><b>契约只依赖 SPI 公共表面</b>，不依赖任何具体 Redis 客户端，因此可脱离 Redis 服务运行。</p>
 *
 * <p><b>脚本能力的核心契约</b>是 {@code supportsScript()} 与 {@code eval()} 的一致性：
 * <ul>
 *   <li>支持脚本 → {@code eval} 不得抛 {@link UnsupportedOperationException}；</li>
 *   <li>不支持脚本 → {@code eval} 必须抛 {@link UnsupportedOperationException}；</li>
 *   <li>{@code eval(null, ...)} 必须快速失败（抛运行时异常），不得返回 {@code null} 静默吞掉。</li>
 * </ul>
 * 这三条把「实现方忘记重写 {@code supportsScript()}」这类最容易犯的错变成一条可执行断言。</p>
 *
 * <p><b>行为纪律（推荐实现遵守，非强制断言）</b>：{@code setIfAbsent} 在键已存在时不应刷新原 TTL；
 * 本套件不对 TTL 做跨实现断言（需时间自省，mock 客户端无法提供），由各自实现保证。</p>
 *
 * @author 王飞
 * @since 0.0.2
 */
abstract class AbstractRedisCommandExecutorContractTest {

    /** 待测实现，每个用例通过本方法获取全新实例。 */
    protected abstract RedisCommandExecutor createExecutor();

    /**
     * 执行全部契约项（基于 {@link #createExecutor()} 新建实例），返回失败信息列表。
     *
     * @return 失败描述；完全合规时为空列表
     */
    protected List<String> verify() {
        return verify(createExecutor());
    }

    /**
     * 对指定实现执行全部契约项，返回失败信息列表（空列表表示完全合规）。
     * 便于对「故意不合规」的实现做分支覆盖。
     *
     * @param executor 待测实现（非 {@code null}）
     * @return 失败描述；完全合规时为空列表
     */
    protected List<String> verify(RedisCommandExecutor executor) {
        List<String> failures = new ArrayList<>();
        try {
            checkSetIfAbsentNewKey(executor, failures);
            checkSetIfAbsentExistingKey(executor, failures);
            checkGetAndDeleteRoundTrip(executor, failures);
            checkScriptCapabilityConsistency(executor, failures);
        } catch (Exception e) {
            failures.add("契约执行异常: " + e);
        }
        return failures;
    }

    private void checkSetIfAbsentNewKey(RedisCommandExecutor ex, List<String> failures) {
        String key = "contract:setnew:" + System.nanoTime();
        if (!ex.setIfAbsent(key, "v", 1000L)) {
            failures.add("A1: setIfAbsent 新键应返回 true");
        }
        if (!"v".equals(ex.get(key))) {
            failures.add("A1: get 应返回刚写入的值");
        }
    }

    private void checkSetIfAbsentExistingKey(RedisCommandExecutor ex, List<String> failures) {
        String key = "contract:setexist:" + System.nanoTime();
        if (!ex.setIfAbsent(key, "first", 1000L)) {
            failures.add("A2: 首次 setIfAbsent 应返回 true");
            return;
        }
        if (ex.setIfAbsent(key, "second", 1000L)) {
            failures.add("A2: 已存在键的 setIfAbsent 应返回 false");
        }
        if (!"first".equals(ex.get(key))) {
            failures.add("A2: 已存在键的值不应被覆盖");
        }
    }

    private void checkGetAndDeleteRoundTrip(RedisCommandExecutor ex, List<String> failures) {
        String key = "contract:del:" + System.nanoTime();
        ex.setIfAbsent(key, "x", 1000L);
        ex.delete(key);
        if (ex.get(key) != null) {
            failures.add("A3: delete 后 get 应返回 null");
        }
    }

    private void checkScriptCapabilityConsistency(RedisCommandExecutor ex, List<String> failures) {
        boolean supports = ex.supportsScript();

        if (supports) {
            // B1：支持脚本则 eval 不得抛 UnsupportedOperationException
            try {
                Object r = ex.eval(FakeRedisCommandExecutor.SCRIPT_FIXED_WINDOW,
                        List.of("contract:scriptk:" + System.nanoTime()), List.of("1", "1000"));
                if (r != null && !(r instanceof Number || r instanceof String
                        || r instanceof List || r instanceof Boolean)) {
                    failures.add("B1: eval 返回值类型不受支持: " + r.getClass());
                }
            } catch (UnsupportedOperationException e) {
                failures.add("B1: supportsScript()==true 但 eval 抛 UnsupportedOperationException");
            } catch (Exception ignored) {
                // 其它异常（如脚本未预置）允许，只要不是 UOE
            }
        } else {
            // B2：不支持脚本则 eval 必须抛 UnsupportedOperationException
            try {
                ex.eval("any script", List.of("k"), List.of("a"));
                failures.add("B2: supportsScript()==false 但 eval 未抛 UnsupportedOperationException");
            } catch (UnsupportedOperationException expected) {
                // 合规
            } catch (Exception e) {
                failures.add("B2: 不支持脚本时 eval 应抛 UnsupportedOperationException，实际抛 " + e.getClass());
            }
        }

        // B3：null script 必须快速失败，不得返回 null 静默
        try {
            Object r = ex.eval(null, List.of("k"), List.of("a"));
            if (r == null) {
                failures.add("B3: eval(null,...) 不得返回 null 静默");
            } else {
                failures.add("B3: eval(null,...) 不得返回非 null 值，实际=" + r);
            }
        } catch (RuntimeException expected) {
            // 合规：抛 NPE / IAE / BusinessException 等运行时异常均可
        }
    }

    /**
     * 自带测试：合规实现（由子类 {@link #createExecutor()} 提供）应全部契约通过。
     */
    @Test
    void compliantImplementationPassesAllContracts() {
        List<String> failures = verify();
        assertThat(failures)
                .as("契约失败项: " + failures)
                .isEmpty();
    }
}
