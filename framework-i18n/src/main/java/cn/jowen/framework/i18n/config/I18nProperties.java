package cn.jowen.framework.i18n.config;

import cn.jowen.framework.i18n.reload.ReloadStrategy;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 国际化配置属性（纯 POJO）。绑定前缀 {@code framework.i18n.*} 由 boot-autoconfigure 的
 * {@code BootI18nProperties} 完成。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class I18nProperties {

    /**
     * 是否启用框架国际化装配，缺省开启。
     */
    private boolean enabled = true;

    /**
     * 消息源类型，缺省 {@link SourceType#PROPERTIES}。
     */
    private SourceType source = SourceType.PROPERTIES;

    /**
     * 资源包基名（classpath 下，点分隔），如 {@code i18n.messages}，缺省 {@code messages}。
     */
    private String basename = "messages";

    /**
     * 区域解析策略，缺省 {@link ResolverType#PARAMETER}。
     */
    private ResolverType resolver = ResolverType.PARAMETER;

    /**
     * 消息格式化策略，缺省 {@link FormatterType#JAVA_TEXT}。
     */
    private FormatterType formatter = FormatterType.JAVA_TEXT;

    /**
     * 默认区域，如 {@code zh_CN}，缺省使用 JVM 默认区域。
     */
    private String defaultLocale = "";

    /**
     * 支持的区域列表，空表示不限制。
     */
    private List<String> supportedLocales = List.of();

    /**
     * 多源聚合查找顺序（{@link SourceType#COMPOSITE} 时生效），依次命中即返回。
     */
    private List<SourceType> compositeOrder = List.of();

    /**
     * 消息资源缓存秒数，缺省 3600。
     */
    private long cacheSeconds = 3600;

    /**
     * 资源热加载策略，缺省 {@link ReloadStrategy#MANUAL}（仅手动触发）。
     */
    private ReloadStrategy reloadStrategy = ReloadStrategy.MANUAL;

    /**
     * 热加载轮询间隔（秒），0 表示不轮询，缺省 0。
     */
    private long reloadIntervalSeconds = 0;

    /**
     * 数据库消息源配置。
     */
    private DatabaseSource database = new DatabaseSource();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public ResolverType getResolver() {
        return resolver;
    }

    public void setResolver(ResolverType resolver) {
        this.resolver = resolver;
    }

    public FormatterType getFormatter() {
        return formatter;
    }

    public void setFormatter(FormatterType formatter) {
        this.formatter = formatter;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(List<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public List<SourceType> getCompositeOrder() {
        return compositeOrder;
    }

    public void setCompositeOrder(List<SourceType> compositeOrder) {
        this.compositeOrder = compositeOrder;
    }

    public long getCacheSeconds() {
        return cacheSeconds;
    }

    public void setCacheSeconds(long cacheSeconds) {
        this.cacheSeconds = cacheSeconds;
    }

    public ReloadStrategy getReloadStrategy() {
        return reloadStrategy;
    }

    public void setReloadStrategy(ReloadStrategy reloadStrategy) {
        this.reloadStrategy = reloadStrategy;
    }

    public long getReloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

    public void setReloadIntervalSeconds(long reloadIntervalSeconds) {
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    public DatabaseSource getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseSource database) {
        this.database = database;
    }

    /**
     * 数据库消息源配置（{@code framework.i18n.database.*}），与 {@link cn.jowen.framework.i18n.source.DatabaseMessageSource}
     * 的表结构约定对应。
     */
    @NullMarked
    public static class DatabaseSource {

        /**
         * 消息表名，缺省 {@code i18n_message}。
         */
        private String tableName = "i18n_message";

        /**
         * 区域列名，缺省 {@code locale}。
         */
        private String localeColumn = "locale";

        /**
         * 消息编码列名，缺省 {@code code}。
         */
        private String codeColumn = "code";

        /**
         * 消息内容列名，缺省 {@code message}。
         */
        private String messageColumn = "message";

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getLocaleColumn() {
            return localeColumn;
        }

        public void setLocaleColumn(String localeColumn) {
            this.localeColumn = localeColumn;
        }

        public String getCodeColumn() {
            return codeColumn;
        }

        public void setCodeColumn(String codeColumn) {
            this.codeColumn = codeColumn;
        }

        public String getMessageColumn() {
            return messageColumn;
        }

        public void setMessageColumn(String messageColumn) {
            this.messageColumn = messageColumn;
        }
    }
}