package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.config.FormatterType;
import cn.jowen.framework.i18n.config.SourceType;
import io.micrometer.core.instrument.MeterRegistry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link I18nAutoConfiguration#messageSource} DATABASE 源可用分支测试。
 *
 * <p>既有 {@code I18nAutoConfigurationTest} 只验证了「DATABASE 源 + 不可用数据源 → 抛出」与
 * 「DATABASE 源 + 无数据源 → 回退 Properties」两条分支，真实可查询的 {@code DatabaseMessageSource}
 * 装配路径（构造、格式化器应用、加入组合源）未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class I18nAutoConfigurationGapTest {

    private static final String TABLE = "i18n_message";

    @Test
    void databaseSource_withQueryableDataSource_isRegisteredInComposite() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.DATABASE);
        props.setFormatter(FormatterType.NAMED_PARAMETER);

        DataSource dataSource = createMessageTable();

        MessageSource source = new I18nAutoConfiguration().messageSource(
                props, dataSource, null, nullProvider(MeterRegistry.class),
                nullProvider(RedissonClient.class));

        assertThat(source).isNotNull();
        assertThat(source.contains("greeting", Locale.of("zh", "CN"))).isTrue();
    }

    @Test
    void databaseSource_withoutDataSource_fallsBackToPropertiesSource() {
        BootI18nProperties props = new BootI18nProperties();
        props.setSource(SourceType.DATABASE);

        MessageSource source = new I18nAutoConfiguration().messageSource(
                props, null, null, nullProvider(MeterRegistry.class),
                nullProvider(RedissonClient.class));

        assertThat(source).isNotNull();
        assertThat(source.contains("greeting", Locale.of("zh", "CN"))).isFalse();
    }

    /** 建表并写入一行中文文案，供 {@code DatabaseMessageSource} 构造期 reload 读取。 */
    private static DataSource createMessageTable() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:i18n-gap-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // 列名不加引号：H2 归一化为大写，框架 resultSetToMaps 再统一小写为 locale/code/message
            statement.execute("CREATE TABLE " + TABLE + " ("
                    + "locale VARCHAR(32), code VARCHAR(128), message VARCHAR(512))");
            statement.execute("INSERT INTO " + TABLE + " VALUES ('zh_CN','greeting','你好')");
        } catch (Exception e) {
            throw new IllegalStateException("初始化 H2 消息表失败", e);
        }
        return dataSource;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ObjectProvider<T> nullProvider(Class<T> type) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
