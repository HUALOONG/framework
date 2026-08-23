package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.api.MessageSource;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CompositeMessageSource} 测试。
 */
class CompositeMessageSourceTest {

    private final CompositeMessageSource composite = new CompositeMessageSource();

    @Test
    void getMessage_firstSourceHit_returnsMessage() {
        composite.add(createSource("key1", "from-source-1"));
        composite.add(createSource("key1", "from-source-2"));
        String result = composite.getMessage("key1", Locale.ENGLISH, null);
        assertThat(result).isEqualTo("from-source-1");
    }

    @Test
    void getMessage_firstSourceMiss_secondSourceHit() {
        composite.add(createSource("key1", "source1"));
        composite.add(createSource("key2", "source2"));
        String result = composite.getMessage("key2", Locale.ENGLISH, null);
        assertThat(result).isEqualTo("source2");
    }

    @Test
    void getMessage_allSourcesMiss_returnsNull() {
        composite.add(createSource("key1", "source1"));
        String result = composite.getMessage("nonexistent", Locale.ENGLISH, null);
        assertThat(result).isNull();
    }

    @Test
    void contains_anySourceHas_returnsTrue() {
        composite.add(createSource("key1", "source1"));
        assertThat(composite.contains("key1", Locale.ENGLISH)).isTrue();
    }

    @Test
    void contains_noSourceHas_returnsFalse() {
        composite.add(createSource("key1", "source1"));
        assertThat(composite.contains("nonexistent", Locale.ENGLISH)).isFalse();
    }

    @Test
    void reload_allReloadableSourcesReloaded() {
        int[] reloadCount = {0};
        ReloadableSource reloadable = new ReloadableSource() {
            @Override
            public void reload() {
                reloadCount[0]++;
            }
        };
        composite.add(createSource("key", "val"));
        composite.add(reloadable);
        composite.reload();
        assertThat(reloadCount[0]).isEqualTo(1);
    }

    @Test
    void add_multipleSources() {
        composite.add(createSource("a", "v1"));
        composite.add(createSource("b", "v2"));
        assertThat(composite.getMessage("a", Locale.ENGLISH, null)).isEqualTo("v1");
        assertThat(composite.getMessage("b", Locale.ENGLISH, null)).isEqualTo("v2");
    }

    @Test
    void contains_firstSourceMatch_shortCircuits() {
        int[] callCount = {0};
        composite.add(createSource("key1", "source1"));
        composite.add(new MessageSource() {
            @Override
            public String getMessage(String code, Locale locale, Object[] args) {
                callCount[0]++;
                return null;
            }

            @Override
            public boolean contains(String code, Locale locale) {
                callCount[0]++;
                return false;
            }
        });
        composite.contains("key1", Locale.ENGLISH);
        assertThat(callCount[0]).isEqualTo(0);
    }

    /**
     * 测试用的简单消息源实现。
     */
    private static MessageSource createSource(String code, String value) {
        return new MessageSource() {
            @Override
            public String getMessage(String c, Locale locale, Object[] args) {
                return code.equals(c) ? value : null;
            }

            @Override
            public boolean contains(String c, Locale locale) {
                return code.equals(c);
            }
        };
    }

    private abstract static class ReloadableSource implements MessageSource, cn.jowen.framework.i18n.api.ReloadableMessageSource {
        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            return null;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return false;
        }
    }
}
