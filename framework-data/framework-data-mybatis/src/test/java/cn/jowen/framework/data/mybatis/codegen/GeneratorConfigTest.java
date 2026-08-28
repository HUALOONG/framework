package cn.jowen.framework.data.mybatis.codegen;

import cn.jowen.framework.data.core.mapping.NamingStrategy;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 覆盖 {@link GeneratorConfig} 及其内部 {@code Builder} 与默认 {@code NamingStrategy} 实现
 * （纯配置 POJO，无运行期依赖）。不触发任何代码生成（{@code EntityGenerator} 等需表元数据）。
 *
 * @author software-engineer-2
 */
class GeneratorConfigTest {

    @Test
    void defaults_usesSensiblePackages() {
        GeneratorConfig config = GeneratorConfig.defaults();
        assertThat(config.getEntityPackage()).isEqualTo("com.example.entity");
        assertThat(config.getMapperPackage()).isEqualTo("com.example.mapper");
        assertThat(config.getTableDefPackage()).isEqualTo("com.example.def");
        assertThat(config.getOutputDir()).isEqualTo(Paths.get("src/main/java"));
    }

    @Test
    void settersAndGetters_propagateValues() {
        GeneratorConfig config = new GeneratorConfig();
        config.setEntityPackage("pkg.entity");
        config.setMapperPackage("pkg.mapper");
        config.setTableDefPackage("pkg.def");
        config.setOutputDir(Paths.get("generated"));

        NamingStrategy custom = new NamingStrategy() {
            @Override
            public String toTableName(String className) {
                return "TBL_" + className;
            }

            @Override
            public String toColumnName(String fieldName) {
                return "COL_" + fieldName;
            }
        };
        config.setNamingStrategy(custom);

        assertThat(config.getEntityPackage()).isEqualTo("pkg.entity");
        assertThat(config.getMapperPackage()).isEqualTo("pkg.mapper");
        assertThat(config.getTableDefPackage()).isEqualTo("pkg.def");
        assertThat(config.getOutputDir()).isEqualTo(Paths.get("generated"));
        assertThat(config.namingStrategy()).isSameAs(custom);
    }

    @Test
    void defaultNamingStrategy_camelToLowerSnakeCase() {
        GeneratorConfig config = new GeneratorConfig();
        NamingStrategy ns = config.namingStrategy();

        assertThat(ns.toTableName("OrderItem")).isEqualTo("order_item");
        assertThat(ns.toTableName("User")).isEqualTo("user");
        assertThat(ns.toTableName("SysUserRole")).isEqualTo("sys_user_role");
        assertThat(ns.toTableName("ID")).isEqualTo("i_d");

        assertThat(ns.toColumnName("orderItem")).isEqualTo("order_item");
        assertThat(ns.toColumnName("userName")).isEqualTo("user_name");
        assertThat(ns.toColumnName("createdAt")).isEqualTo("created_at");
    }

    @Test
    void builder_assemblesConfig() {
        NamingStrategy ns = new NamingStrategy() {
            @Override
            public String toTableName(String className) {
                return className;
            }

            @Override
            public String toColumnName(String fieldName) {
                return fieldName;
            }
        };

        GeneratorConfig config = GeneratorConfig.builder()
                .entityPackage("b.entity")
                .mapperPackage("b.mapper")
                .tableDefPackage("b.def")
                .outputDir(Paths.get("build"))
                .namingStrategy(ns)
                .build();

        assertThat(config.getEntityPackage()).isEqualTo("b.entity");
        assertThat(config.getMapperPackage()).isEqualTo("b.mapper");
        assertThat(config.getTableDefPackage()).isEqualTo("b.def");
        assertThat(config.getOutputDir()).isEqualTo(Paths.get("build"));
        assertThat(config.namingStrategy()).isSameAs(ns);
    }

    @Test
    void createGenerators_returnNonNullWithConfig() {
        GeneratorConfig config = GeneratorConfig.defaults();
        assertThat(config.createEntityGenerator()).isNotNull();
        assertThat(config.createMapperGenerator()).isNotNull();
        assertThat(config.createTableDefGenerator()).isNotNull();
    }
}
