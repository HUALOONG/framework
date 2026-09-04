package cn.jowen.framework.data.mybatis.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableDefGeneratorTest {

    @TempDir
    Path tempDir;

    private EntityGenerator.TableInfo sampleTable() {
        return new EntityGenerator.TableInfo("sys_user_order", List.of(
                new EntityGenerator.ColumnInfo("id", "bigint", true, true),
                new EntityGenerator.ColumnInfo("user_name", "varchar", false, false),
                new EntityGenerator.ColumnInfo("age", "int", false, false),
                new EntityGenerator.ColumnInfo("active", "tinyint", false, false),
                new EntityGenerator.ColumnInfo("balance", "decimal", false, false),
                new EntityGenerator.ColumnInfo("unknown_col", "weirdtype", false, false)
        ));
    }

    @Test
    void generate_emitsTableDefWithFields() {
        TableDefGenerator gen = new TableDefGenerator(GeneratorConfig.defaults());
        String code = gen.generate(sampleTable());

        assertThat(code).contains("package com.example.def;");
        assertThat(code).contains("public final class SysUserOrderDef");
        assertThat(code).contains("private SysUserOrderDef() {}");
        assertThat(code).contains("public static final TableField<SysUserOrder, Long> id");
        assertThat(code).contains("public static final TableField<SysUserOrder, String> userName");
        assertThat(code).contains("public static final TableField<SysUserOrder, Integer> age");
        assertThat(code).contains("public static final TableField<SysUserOrder, Boolean> active");
        assertThat(code).contains("public static final TableField<SysUserOrder, java.math.BigDecimal> balance");
        // 未知类型回退 String
        assertThat(code).contains("public static final TableField<SysUserOrder, String> unknownCol");
    }

    @Test
    void tableField_getColumnName() {
        TableDefGenerator.TableField<Object, Object> f = new TableDefGenerator.TableField<>("col_x");
        assertThat(f.getColumnName()).isEqualTo("col_x");
    }

    @Test
    void generate_nullTableInfo_throws() {
        TableDefGenerator gen = new TableDefGenerator(GeneratorConfig.defaults());
        assertThatThrownBy(() -> gen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableInfo must not be null");
    }

    @Test
    void constructor_nullConfig_throws() {
        assertThatThrownBy(() -> new TableDefGenerator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config must not be null");
    }

    @Test
    void generateToFile_writesJavaFile() throws Exception {
        GeneratorConfig config = GeneratorConfig.builder()
                .tableDefPackage("com.example.def")
                .outputDir(tempDir)
                .build();
        TableDefGenerator gen = new TableDefGenerator(config);
        gen.generateToFile(sampleTable());

        Path out = tempDir.resolve("com/example/def/SysUserOrderDef.java");
        assertThat(Files.exists(out)).isTrue();
        assertThat(Files.readString(out)).contains("public final class SysUserOrderDef");
    }
}
