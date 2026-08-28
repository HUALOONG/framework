package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HashStrategy} 内容摘要、去重与降级验证（补充 factory 测试未覆盖的用例）。
 */
class HashStrategyTest {

    private final HashStrategy strategy = new HashStrategy();

    @Test
    void sameContentProducesStableKeyEnablingDedup() {
        byte[] c1 = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] c2 = "hello".getBytes(StandardCharsets.UTF_8);

        assertThat(strategy.generate("a.bin", c1)).isEqualTo(strategy.generate("b.bin", c2));
    }

    @Test
    void keyEqualsMd5ShardWithExtension() {
        // MD5("hello") = 5d41402abc4b2a76b9719d911017c592
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        assertThat(strategy.generate("a.bin", content))
                .isEqualTo("5d/5d41402abc4b2a76b9719d911017c592.bin");
    }

    @Test
    void differentContentProducesDifferentKey() {
        byte[] c1 = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] c2 = "world".getBytes(StandardCharsets.UTF_8);
        assertThat(strategy.generate("a.bin", c1)).isNotEqualTo(strategy.generate("a.bin", c2));
    }

    @Test
    void nullContentFallsBackToUuidWithoutShard() {
        String key = strategy.generate("a.bin", null);
        assertThat(key).doesNotContain("/");
        assertThat(key).endsWith(".bin");
    }

    @Test
    void emptyContentFallsBackToUuid() {
        String key = strategy.generate("a.bin", new byte[0]);
        assertThat(key).doesNotContain("/");
    }

    @Test
    void preservesExtensionInHashPath() {
        byte[] content = "data".getBytes(StandardCharsets.UTF_8);
        assertThat(strategy.generate("report.pdf", content)).endsWith(".pdf");
    }

    @Test
    void typeIsHashEvenWhenFallingBack() {
        assertThat(strategy.type()).isEqualTo(NamingStrategy.HASH);
        assertThat(strategy.generate("a.bin", null)).isNotNull();
    }
}
