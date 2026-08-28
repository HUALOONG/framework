package cn.jowen.framework.data.mybatis.codegen;

import org.jspecify.annotations.NullMarked;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@NullMarked
/**
 * 「EntityGenerator」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class EntityGenerator {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(EntityGenerator.class);
    /** config 不可变字段。 */
    private final GeneratorConfig config;

    /**
     * 构造实例。
     * @param config 参数 config
     */
    public EntityGenerator(GeneratorConfig config) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        this.config = config;
    }

    /**
     * 执行generate操作。
     * @param tableInfo 参数 tableInfo
     * @return 结果
     */
    public String generate(TableInfo tableInfo) {
        if (tableInfo == null) throw new IllegalArgumentException("tableInfo must not be null");
        String className = toClassName(tableInfo.tableName());
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(config.getEntityPackage()).append(";\n\n");
        sb.append("import cn.jowen.framework.data.core.meta.Column;\n");
        sb.append("import cn.jowen.framework.data.core.meta.GeneratedValue;\n");
        sb.append("import cn.jowen.framework.data.core.meta.Id;\n");
        sb.append("import cn.jowen.framework.data.core.meta.Table;\n");
        sb.append("import org.jspecify.annotations.NullMarked;\n\n");
        sb.append("@Table(\"").append(tableInfo.tableName()).append("\")\n");
        sb.append("@NullMarked\n");
        sb.append("public class ").append(className).append(" {\n\n");
        for (ColumnInfo col : tableInfo.columns()) {
            if (col.primaryKey()) {
                sb.append("    @Id");
                if (col.autoIncrement()) sb.append("(keyType = cn.jowen.framework.data.core.meta.KeyType.Auto)");
                sb.append("\n    ");
            }
            sb.append(toJavaType(col.type())).append(" ").append(toFieldName(col.name())).append(";\n\n");
        }
        sb.append("}\n");
        logger.info("生成实体类: " + className);
        return sb.toString();
    }

    /**
     * 执行generate to file操作。
     * @param tableInfo 参数 tableInfo
     * @throws IOException IOException 异常
     */
    public void generateToFile(TableInfo tableInfo) throws IOException {
        String code = generate(tableInfo);
        String className = toClassName(tableInfo.tableName());
        Path outputDir = config.getOutputDir().resolve(config.getEntityPackage().replace('.', '/'));
        Files.createDirectories(outputDir);
        try (BufferedWriter writer = Files.newBufferedWriter(outputDir.resolve(className + ".java"))) {
            writer.write(code);
        }
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

    private static String toJavaType(String sqlType) {
        return switch (sqlType.toLowerCase()) {
            case "varchar", "char", "text", "longtext", "tinytext", "mediumtext" -> "String";
            case "int", "integer", "smallint", "mediumint" -> "Integer";
            case "bigint" -> "Long";
            case "tinyint" -> "Boolean";
            case "decimal", "numeric" -> "java.math.BigDecimal";
            case "float" -> "Float";
            case "double" -> "Double";
            case "date" -> "java.time.LocalDate";
            case "datetime", "timestamp" -> "java.time.LocalDateTime";
            default -> "String";
        };
    }

    /**
     * 「TableInfo」不可变数据载体。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public record TableInfo(String tableName, List<ColumnInfo> columns) {}
    /**
     * 「ColumnInfo」不可变数据载体。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public record ColumnInfo(String name, String type, boolean primaryKey, boolean autoIncrement) {}
}
