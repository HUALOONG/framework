package cn.jowen.framework.data.mybatis.codegen;

import cn.jowen.framework.data.core.mapping.NamingStrategy;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.nio.file.Paths;

@NullMarked
/**
 * 「Generator」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class GeneratorConfig {

    /** entityPackage 字段。 */
    private String entityPackage = "com.example.entity";
    /** mapperPackage 字段。 */
    private String mapperPackage = "com.example.mapper";
    /** tableDefPackage 字段。 */
    private String tableDefPackage = "com.example.def";
    /** outputDir 字段。 */
    private Path outputDir = Paths.get("src/main/java");
    /** namingStrategy 字段。 */
    private NamingStrategy namingStrategy = new NamingStrategy() {
        @Override public String toTableName(String className) { return toLowerSnakeCase(className); }
        @Override public String toColumnName(String fieldName) { return toLowerSnakeCase(fieldName); }
        private String toLowerSnakeCase(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isUpperCase(c) && i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            }
            return sb.toString();
        }
    };

    /** public 字段。 */
    public GeneratorConfig() {}

    /** new 静态变量。 */
    public static GeneratorConfig defaults() { return new GeneratorConfig(); }

    /** entityPackage 字段。 */
    public String getEntityPackage() { return entityPackage; }
    /** void 字段。 */
    public void setEntityPackage(String v) { this.entityPackage = v; }
    /** mapperPackage 字段。 */
    public String getMapperPackage() { return mapperPackage; }
    /** void 字段。 */
    public void setMapperPackage(String v) { this.mapperPackage = v; }
    /** tableDefPackage 字段。 */
    public String getTableDefPackage() { return tableDefPackage; }
    /** void 字段。 */
    public void setTableDefPackage(String v) { this.tableDefPackage = v; }
    /** outputDir 字段。 */
    public Path getOutputDir() { return outputDir; }
    /** void 字段。 */
    public void setOutputDir(Path v) { this.outputDir = v; }
    /** namingStrategy 字段。 */
    public NamingStrategy namingStrategy() { return namingStrategy; }
    /** void 字段。 */
    public void setNamingStrategy(NamingStrategy v) { this.namingStrategy = v; }

    /** new 静态变量。 */
    public static Builder builder() { return new Builder(); }

    /** new 字段。 */
    public EntityGenerator createEntityGenerator() { return new EntityGenerator(this); }
    /** new 字段。 */
    public MapperGenerator createMapperGenerator() { return new MapperGenerator(this); }
    /** new 字段。 */
    public TableDefGenerator createTableDefGenerator() { return new TableDefGenerator(this); }

    /**
     * 「」封装相关能力。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public static final class Builder {
        /** config 不可变字段。 */
        private final GeneratorConfig config = new GeneratorConfig();
        /** Builder 字段。 */
        public Builder entityPackage(String pkg) { config.entityPackage = pkg; return this; }
        /** Builder 字段。 */
        public Builder mapperPackage(String pkg) { config.mapperPackage = pkg; return this; }
        /** Builder 字段。 */
        public Builder tableDefPackage(String pkg) { config.tableDefPackage = pkg; return this; }
        /** Builder 字段。 */
        public Builder outputDir(Path dir) { config.outputDir = dir; return this; }
        /** Builder 字段。 */
        public Builder namingStrategy(NamingStrategy ns) { config.namingStrategy = ns; return this; }
        /** config 字段。 */
        public GeneratorConfig build() { return config; }
    }
}
