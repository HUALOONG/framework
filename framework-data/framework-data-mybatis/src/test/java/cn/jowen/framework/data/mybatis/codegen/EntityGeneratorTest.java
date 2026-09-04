package cn.jowen.framework.data.mybatis.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityGeneratorTest {

    @TempDir
    Path tempDir;

    private EntityGenerator.TableInfo sampleTable() {
        return new EntityGenerator.TableInfo("sys_user_order", List.of(
                new EntityGenerator.ColumnInfo("id", "bigint", true, true),
                new EntityGenerator.ColumnInfo("user_name", "varchar", false, false),
                new EntityGenerator.ColumnInfo("age", "int", false, false),
                new EntityGenerator.ColumnInfo("balance", "decimal", false, false),
                new EntityGenerator.ColumnInfo("score", "float", false, false),
                new EntityGenerator.ColumnInfo("ratio", "double", false, false),
                new EntityGenerator.ColumnInfo("active", "tinyint", false, false),
                new EntityGenerator.ColumnInfo("birthday", "date", false, false),
                new EntityGenerator.ColumnInfo("created_at", "datetime", false, false),
                new EntityGenerator.ColumnInfo("bio", "text", false, false),
                new EntityGenerator.ColumnInfo("legacy_flag", "integer", false, false),
                new EntityGenerator.ColumnInfo("unknown_col", "weirdtype", false, false)
        ));
    }

    @Test
    void generate_mapsAllSqlTypesAndKey() {
        EntityGenerator gen = new EntityGenerator(GeneratorConfig.defaults());
        String code = gen.generate(sampleTable());

        assertThat(code).contains("package com.example.entity;");
        assertThat(code).contains("@Table(\"sys_user_order\")");
        assertThat(code).contains("public class SysUserOrder");
        assertThat(code).contains("@Id(keyType = cn.jowen.framework.data.core.meta.KeyType.Auto)");
        // 类型映射
        assertThat(code).contains("Long id;");
        assertThat(code).contains("String userName;");
        assertThat(code).contains("Integer age;");
        assertThat(code).contains("java.math.BigDecimal balance;");
        assertThat(code).contains("Float score;");
        assertThat(code).contains("Double ratio;");
        assertThat(code).contains("Boolean active;");
        assertThat(code).contains("java.time.LocalDate birthday;");
        assertThat(code).contains("java.time.LocalDateTime createdAt;");
        assertThat(code).contains("String bio;");
        assertThat(code).contains("Integer legacyFlag;");
        // 未知类型回退 String
        assertThat(code).contains("String unknownCol;");
    }

    @Test
    void generate_primaryKeyWithoutAutoIncrement() {
        EntityGenerator gen = new EntityGenerator(GeneratorConfig.defaults());
        EntityGenerator.TableInfo table = new EntityGenerator.TableInfo("t",
                List.of(new EntityGenerator.ColumnInfo("code", "varchar", true, false)));
        String code = gen.generate(table);
        assertThat(code).contains("@Id");
        assertThat(code).doesNotContain("keyType");
    }

    @Test
    void generate_nullTableInfo_throws() {
        EntityGenerator gen = new EntityGenerator(GeneratorConfig.defaults());
        assertThatThrownBy(() -> gen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableInfo must not be null");
    }

    @Test
    void constructor_nullConfig_throws() {
        assertThatThrownBy(() -> new EntityGenerator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config must not be null");
    }

    @Test
    void generateToFile_writesJavaFile() throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .entityPackage("com.example.entity")
                .outputDir(tempDir)
                .build();
        EntityGenerator gen = new EntityGenerator(config);
        gen.generateToFile(sampleTable());

        Path out = tempDir.resolve("com/example/entity/SysUserOrder.java");
        assertThat(Files.exists(out)).isTrue();
        String written = Files.readString(out);
        assertThat(written).contains("public class SysUserOrder");
    }
}
