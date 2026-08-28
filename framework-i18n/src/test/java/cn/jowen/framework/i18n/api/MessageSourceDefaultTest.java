package cn.jowen.framework.i18n.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MessageSource} 默认方法测试。
 */
class MessageSourceDefaultTest {

    private Locale previous;

    @BeforeEach
    void saveContextLocale() {
        previous = LocaleContextHolder.getLocale();
    }

    @AfterEach
    void restoreContextLocale() {
        LocaleContextHolder.setLocale(previous);
    }

    @Test
    void getMessage_withContextLocale() {
        LocaleContextHolder.setLocale(Locale.FRANCE);
        RecordingSource source = new RecordingSource("bonjour");

        assertThat(source.getMessage("greeting", new Object[]{"x"})).isEqualTo("bonjour");
        assertThat(source.lastLocale).isEqualTo(Locale.FRANCE);
    }

    @Test
    void getMessage_resolvable_hit() {
        RecordingSource source = new RecordingSource("Hello");
        MessageSourceResolvable resolvable = new MessageSourceResolvable("greeting", "fallback");

        assertThat(source.getMessage(resolvable, Locale.US)).isEqualTo("Hello");
    }

    @Test
    void getMessage_resolvable_miss_usesDefault() {
        RecordingSource source = new RecordingSource(null);
        MessageSourceResolvable resolvable = new MessageSourceResolvable("greeting", "fallback");

        assertThat(source.getMessage(resolvable, Locale.US)).isEqualTo("fallback");
    }

    @Test
    void getMessage_resolvable_miss_noDefault_returnsNull() {
        RecordingSource source = new RecordingSource(null);
        MessageSourceResolvable resolvable = new MessageSourceResolvable("greeting");

        assertThat(source.getMessage(resolvable, Locale.US)).isNull();
    }

    @Test
    void getMessage_resolvable_withArgs_passesArgs() {
        RecordingSource source = new RecordingSource("Welcome, {0}");
        MessageSourceResolvable resolvable =
                new MessageSourceResolvable("welcome", new Object[]{"Tom"}, null);

        assertThat(source.getMessage(resolvable, Locale.US)).isEqualTo("Welcome, {0}");
        assertThat(source.lastArgs).containsExactly("Tom");
    }

    @Test
    void getMessageRequired_hit() {
        RecordingSource source = new RecordingSource("Hello");

        assertThat(source.getMessageRequired("greeting", Locale.US, null)).isEqualTo("Hello");
    }

    @Test
    void getMessageRequired_miss_throws() {
        RecordingSource source = new RecordingSource(null);

        assertThatThrownBy(() -> source.getMessageRequired("missing", Locale.US, null))
                .isInstanceOf(MessageNotFoundException.class);
    }

    /**
     * 记录传入区域与参数的最小桩实现。
     */
    private static final class RecordingSource implements MessageSource {

        private final String value;
        private Locale lastLocale;
        private Object[] lastArgs;

        RecordingSource(String value) {
            this.value = value;
        }

        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            lastLocale = locale;
            lastArgs = args;
            return value;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return value != null;
        }
    }
}
