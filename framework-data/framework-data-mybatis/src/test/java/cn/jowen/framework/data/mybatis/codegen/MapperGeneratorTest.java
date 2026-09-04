package cn.jowen.framework.data.mybatis.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapperGeneratorTest {

    @TempDir
    Path tempDir;

    private EntityGenerator.TableInfo sampleTable() {
        return new EntityGenerator.TableInfo("sys_user_order", List.of(
                new EntityGenerator.ColumnInfo("id", "bigint", true, true)));
    }

    @Test
    void generate_producesBaseMapperInterface() {
        MapperGenerator gen = new MapperGenerator(GeneratorConfig.defaults());
        String code = gen.generate(sampleTable());
        assertThat(code).contains("package com.example.mapper;");
        assertThat(code).contains("import com.mybatisflex.core.BaseMapper;");
        assertThat(code).contains(
                "public interface SysUserOrderMapper extends BaseMapper<SysUserOrder> {}");
    }

    @Test
    void generate_multiPartTableName_convertsCamelCase() {
        EntityGenerator.TableInfo t = new EntityGenerator.TableInfo("t_usr_log", List.of());
        String code = new MapperGenerator(GeneratorConfig.defaults()).generate(t);
        assertThat(code).contains("TUsrLogMapper");
    }

    @Test
    void generate_nullTableInfo_throws() {
        MapperGenerator gen = new MapperGenerator(GeneratorConfig.defaults());
        assertThatThrownBy(() -> gen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableInfo must not be null");
    }

    @Test
    void constructor_nullConfig_throws() {
        assertThatThrownBy(() -> new MapperGenerator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config must not be null");
    }

    @Test
    void generateToFile_writesMapperFile() throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .mapperPackage("com.example.mapper")
                .outputDir(tempDir)
                .build();
        new MapperGenerator(config).generateToFile(sampleTable());
        Path out = tempDir.resolve("com/example/mapper/SysUserOrderMapper.java");
        assertThat(Files.exists(out)).isTrue();
        assertThat(Files.readString(out)).contains("public interface SysUserOrderMapper");
    }
}
