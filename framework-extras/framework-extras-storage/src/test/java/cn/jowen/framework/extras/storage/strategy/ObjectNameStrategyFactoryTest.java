package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ObjectNameStrategyFactory} 与四种命名策略验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class ObjectNameStrategyFactoryTest {

    private static final byte[] CONTENT = "hello".getBytes(StandardCharsets.UTF_8);

    @Test
    void factoryReturnsEachBuiltInStrategy() {
        assertThat(ObjectNameStrategyFactory.get(NamingStrategy.ORIGINAL))
                .isInstanceOf(OriginalNameStrategy.class);
        assertThat(ObjectNameStrategyFactory.get(NamingStrategy.UUID))
                .isInstanceOf(UuidStrategy.class);
        assertThat(ObjectNameStrategyFactory.get(NamingStrategy.DATE))
                .isInstanceOf(DatePathStrategy.class);
        assertThat(ObjectNameStrategyFactory.get(NamingStrategy.HASH))
                .isInstanceOf(HashStrategy.class);
    }

    @Test
    void factoryFallsBackToUuidWhenNull() {
        assertThat(ObjectNameStrategyFactory.get(null)).isInstanceOf(UuidStrategy.class);
    }

    @Test
    void originalKeepsFileName() {
        assertThat(new OriginalNameStrategy().generate("dir/photo.jpg", CONTENT))
                .isEqualTo("photo.jpg");
    }

    @Test
    void uuidUsesUuidWithExtension() {
        String key = new UuidStrategy().generate("photo.jpg", CONTENT);
        assertThat(key).endsWith(".jpg");
        assertThat(key).doesNotContain("photo");
    }

    @Test
    void uuidGeneratedKeysAreUnique() {
        UuidStrategy strategy = new UuidStrategy();
        assertThat(strategy.generate("a.jpg", CONTENT))
                .isNotEqualTo(strategy.generate("a.jpg", CONTENT));
    }

    @Test
    void datePathUsesDatePrefix() {
        String key = new DatePathStrategy().generate("photo.png", CONTENT);
        assertThat(key).matches("\\d{4}/\\d{2}/\\d{2}/.+\\.png");
    }

    @Test
    void hashIsStableForSameContent() {
        HashStrategy strategy = new HashStrategy();
        assertThat(strategy.generate("a.bin", CONTENT))
                .isEqualTo(strategy.generate("b.bin", CONTENT));
    }

    @Test
    void hashShardsByFirstTwoChars() {
        String key = new HashStrategy().generate("a.bin", CONTENT);
        assertThat(key).matches("[0-9a-f]{2}/[0-9a-f]{32}\\.bin");
    }

    @Test
    void hashFallsBackToUuidWhenContentMissing() {
        String key = new HashStrategy().generate("a.bin", null);
        assertThat(key).endsWith(".bin").doesNotContain("/");
    }

    @Test
    void strategyTypesMatchEnum() {
        assertThat(new OriginalNameStrategy().type()).isEqualTo(NamingStrategy.ORIGINAL);
        assertThat(new UuidStrategy().type()).isEqualTo(NamingStrategy.UUID);
        assertThat(new DatePathStrategy().type()).isEqualTo(NamingStrategy.DATE);
        assertThat(new HashStrategy().type()).isEqualTo(NamingStrategy.HASH);
    }

    @Test
    void customStrategyCanBeRegistered() {
        ObjectNameStrategy custom = new ObjectNameStrategy() {
            @Override
            public String generate(String originalName, byte[] content) {
                return "fixed/" + originalName;
            }

            @Override
            public NamingStrategy type() {
                return NamingStrategy.CUSTOM;
            }
        };
        ObjectNameStrategyFactory.register(NamingStrategy.CUSTOM, custom);
        assertThat(ObjectNameStrategyFactory.get(NamingStrategy.CUSTOM).generate("x.txt", CONTENT))
                .isEqualTo("fixed/x.txt");
    }
}
