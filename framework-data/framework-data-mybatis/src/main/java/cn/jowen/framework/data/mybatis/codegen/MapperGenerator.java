package cn.jowen.framework.data.mybatis.codegen;

import org.jspecify.annotations.NullMarked;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@NullMarked
public class MapperGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MapperGenerator.class);
    private final GeneratorConfig config;

    public MapperGenerator(GeneratorConfig config) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        this.config = config;
    }

    public String generate(EntityGenerator.TableInfo tableInfo) {
        if (tableInfo == null) throw new IllegalArgumentException("tableInfo must not be null");
        String className = toClassName(tableInfo.tableName());
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(config.getMapperPackage()).append(";\n\n");
        sb.append("import cn.jowen.framework.data.mybatis.repository.FlexRepositoryFactory.BaseMapper;\n\n");
        sb.append("/** ").append(tableInfo.tableName()).append(" 表 Mapper。 */\n");
        sb.append("public interface ").append(className).append("Mapper extends BaseMapper<").append(className).append("> {}\n");
        logger.info("生成 Mapper: " + className + "Mapper");
        return sb.toString();
    }

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
