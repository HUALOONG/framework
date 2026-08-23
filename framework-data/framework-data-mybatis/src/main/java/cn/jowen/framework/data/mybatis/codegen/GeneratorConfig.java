package cn.jowen.framework.data.mybatis.codegen;

import cn.jowen.framework.data.core.mapping.NamingStrategy;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.nio.file.Paths;

@NullMarked
public final class GeneratorConfig {

    private String entityPackage = "com.example.entity";
    private String mapperPackage = "com.example.mapper";
    private String tableDefPackage = "com.example.def";
    private Path outputDir = Paths.get("src/main/java");
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

    public GeneratorConfig() {}

    public static GeneratorConfig defaults() { return new GeneratorConfig(); }

    public String getEntityPackage() { return entityPackage; }
    public void setEntityPackage(String v) { this.entityPackage = v; }
    public String getMapperPackage() { return mapperPackage; }
    public void setMapperPackage(String v) { this.mapperPackage = v; }
    public String getTableDefPackage() { return tableDefPackage; }
    public void setTableDefPackage(String v) { this.tableDefPackage = v; }
    public Path getOutputDir() { return outputDir; }
    public void setOutputDir(Path v) { this.outputDir = v; }
    public NamingStrategy namingStrategy() { return namingStrategy; }
    public void setNamingStrategy(NamingStrategy v) { this.namingStrategy = v; }

    public static Builder builder() { return new Builder(); }

    public EntityGenerator createEntityGenerator() { return new EntityGenerator(this); }
    public MapperGenerator createMapperGenerator() { return new MapperGenerator(this); }
    public TableDefGenerator createTableDefGenerator() { return new TableDefGenerator(this); }

    public static final class Builder {
        private final GeneratorConfig config = new GeneratorConfig();
        public Builder entityPackage(String pkg) { config.entityPackage = pkg; return this; }
        public Builder mapperPackage(String pkg) { config.mapperPackage = pkg; return this; }
        public Builder tableDefPackage(String pkg) { config.tableDefPackage = pkg; return this; }
        public Builder outputDir(Path dir) { config.outputDir = dir; return this; }
        public Builder namingStrategy(NamingStrategy ns) { config.namingStrategy = ns; return this; }
        public GeneratorConfig build() { return config; }
    }
}
