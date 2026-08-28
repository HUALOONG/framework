package cn.jowen.framework.data.mybatis.codegen;

import org.jspecify.annotations.NullMarked;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@NullMarked
/**
 * 「TableDefGenerator」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class TableDefGenerator {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(TableDefGenerator.class);
    /** config 不可变字段。 */
    private final GeneratorConfig config;

    /**
     * 构造实例。
     * @param config 参数 config
     */
    public TableDefGenerator(GeneratorConfig config) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        this.config = config;
    }

    /**
     * 执行generate操作。
     * @param tableInfo 参数 tableInfo
     * @return 结果
     */
    public String generate(EntityGenerator.TableInfo tableInfo) {
        if (tableInfo == null) throw new IllegalArgumentException("tableInfo must not be null");
        String entityClass = toClassName(tableInfo.tableName());
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(config.getTableDefPackage()).append(";\n\n");
        sb.append("import org.jspecify.annotations.NullMarked;\n\n");
        sb.append("@NullMarked\n");
        sb.append("public final class ").append(entityClass).append("Def {\n\n");
        sb.append("    private ").append(entityClass).append("Def() {}\n\n");
        for (EntityGenerator.ColumnInfo col : tableInfo.columns()) {
            String field = toFieldName(col.name());
            sb.append("    public static final TableField<").append(entityClass).append(", ")
              .append(toJavaType(col.type())).append("> ").append(field)
              .append(" = new TableField<>(\"").append(col.name()).append("\");\n");
        }
        sb.append("}\n");
        logger.info("生成 TableDef: " + entityClass + "Def");
        return sb.toString();
    }

    /**
     * 执行generate to file操作。
     * @param tableInfo 参数 tableInfo
     * @throws IOException IOException 异常
     */
    public void generateToFile(EntityGenerator.TableInfo tableInfo) throws IOException {
        String code = generate(tableInfo);
        String entityClass = toClassName(tableInfo.tableName());
        Path outputDir = config.getOutputDir().resolve(config.getTableDefPackage().replace('.', '/'));
        Files.createDirectories(outputDir);
        try (BufferedWriter writer = Files.newBufferedWriter(outputDir.resolve(entityClass + "Def.java"))) {
            writer.write(code);
        }
    }

    private static String toJavaType(String sqlType) {
        return switch (sqlType.toLowerCase()) {
            case "varchar", "char", "text" -> "String";
            case "int", "integer" -> "Integer";
            case "bigint" -> "Long";
            case "tinyint" -> "Boolean";
            case "decimal", "numeric" -> "java.math.BigDecimal";
            default -> "String";
        };
    }

    /**
     * 「TableField」封装相关能力。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public static final class TableField<E, V> {
        /** columnName 不可变字段。 */
        private final String columnName;
        /** public 字段。 */
        public TableField(String columnName) { this.columnName = columnName; }
        /** columnName 字段。 */
        public String getColumnName() { return columnName; }
    }

    private static String toClassName(String tableName) {
        String[] parts = tableName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
    private static String toFieldName(String colName) {
        String[] parts = colName.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) sb.append(parts[i]);
            else sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
