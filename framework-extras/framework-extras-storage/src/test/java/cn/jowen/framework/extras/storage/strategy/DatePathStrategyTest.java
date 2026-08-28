package cn.jowen.framework.extras.storage.strategy;

import cn.jowen.framework.extras.properties.NamingStrategy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DatePathStrategy} 日期前缀与格式验证（补充 factory 测试未覆盖的用例）。
 */
class DatePathStrategyTest {

    private final DatePathStrategy strategy = new DatePathStrategy();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Test
    void keyStartsWithTodayDatePrefix() {
        String today = LocalDate.now().format(FORMATTER);
        String key = strategy.generate("photo.png", null);

        assertThat(key).startsWith(today + "/");
    }

    @Test
    void keyFormatIsDateSlashUuidWithExtension() {
        String key = strategy.generate("photo.png", null);
        assertThat(key).matches("\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.png");
    }

    @Test
    void sameDayKeysDifferByUuid() {
        String a = strategy.generate("a.jpg", null);
        String b = strategy.generate("a.jpg", null);
        assertThat(a).isNotEqualTo(b);
        // 文件名主体必须是 36 位 UUID 而非原始文件名。
        // 不可直接断言 doesNotContain("a.jpg")：UUID 末位随机为 'a' 时会与 ".jpg" 拼出该子串而误判
        assertThat(nameOf(a)).hasSize(36);
    }

    /** 取路径末段并剥去扩展名，得到对象名主体。 */
    private static String nameOf(String key) {
        String file = key.substring(key.lastIndexOf('/') + 1);
        return file.substring(0, file.lastIndexOf('.'));
    }

    @Test
    void typeIsDate() {
        assertThat(strategy.type()).isEqualTo(NamingStrategy.DATE);
    }
}
