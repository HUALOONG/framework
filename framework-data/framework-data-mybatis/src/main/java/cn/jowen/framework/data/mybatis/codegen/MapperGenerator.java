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
 * 「MapperGenerator」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class MapperGenerator {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(MapperGenerator.class);
    /** config 不可变字段。 */
    private final GeneratorConfig config;

    /**
     * 构造实例。
     * @param config 参数 config
     */
    public MapperGenerator(GeneratorConfig config) {
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
        String className = toClassName(tableInfo.tableName());
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(config.getMapperPackage()).append(";\n\n");
        sb.append("import com.mybatisflex.core.BaseMapper;\n\n");
        sb.append("/** ").append(tableInfo.tableName()).append(" 表 Mapper。 */\n");
        sb.append("public interface ").append(className).append("Mapper extends BaseMapper<").append(className).append("> {}\n");
        logger.info("生成 Mapper: " + className + "Mapper");
        return sb.toString();
    }

    /**
     * 执行generate to file操作。
     * @param tableInfo 参数 tableInfo
     * @throws IOException IOException 异常
     */
    public void generateToFile(EntityGenerator.TableInfo tableInfo) throws IOException {
        String code = generate(tableInfo);
        String className = toClassName(tableInfo.tableName());
        Path outputDir = config.getOutputDir().resolve(config.getMapperPackage().replace('.', '/'));
        Files.createDirectories(outputDir);
        try (BufferedWriter writer = Files.newBufferedWriter(outputDir.resolve(className + "Mapper.java"))) {
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
}
