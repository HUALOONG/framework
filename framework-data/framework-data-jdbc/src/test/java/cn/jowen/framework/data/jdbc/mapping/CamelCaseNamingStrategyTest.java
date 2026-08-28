package cn.jowen.framework.data.jdbc.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CamelCaseNamingStrategy} 测试。
 */
class CamelCaseNamingStrategyTest {

    private final CamelCaseNamingStrategy strategy = new CamelCaseNamingStrategy();

    @Test
    void toTableName_camelToSnake() {
        assertThat(strategy.toTableName("User")).isEqualTo("user");
        assertThat(strategy.toTableName("OrderItem")).isEqualTo("order_item");
        assertThat(strategy.toTableName("OrderDetailItem")).isEqualTo("order_detail_item");
    }

    @Test
    void toTableName_removesEntitySuffix() {
        assertThat(strategy.toTableName("UserEntity")).isEqualTo("user");
        assertThat(strategy.toTableName("OrderDO")).isEqualTo("order");
        assertThat(strategy.toTableName("UserPO")).isEqualTo("user");
        assertThat(strategy.toTableName("UserModel")).isEqualTo("user");
        assertThat(strategy.toTableName("UserDTO")).isEqualTo("user");
    }

    @Test
    void toTableName_suffixOnly_keepsName() {
        assertThat(strategy.toTableName("Entity")).isEqualTo("entity");
    }

    @Test
    void toColumnName_snakeCase() {
        assertThat(strategy.toColumnName("userName")).isEqualTo("user_name");
        assertThat(strategy.toColumnName("id")).isEqualTo("id");
        assertThat(strategy.toColumnName("createdAt")).isEqualTo("created_at");
    }

    @Test
    void toColumnName_empty_returnsAsIs() {
        // 空标识符无需转换，直接返回
        assertThat(strategy.toColumnName("")).isEmpty();
    }

    @Test
    void toFieldName_underscoreToCamel() {
        assertThat(strategy.toFieldName("user_name")).isEqualTo("userName");
        assertThat(strategy.toFieldName("created_at")).isEqualTo("createdAt");
        assertThat(strategy.toFieldName("id")).isEqualTo("id");
        assertThat(strategy.toFieldName("a_b_c")).isEqualTo("aBC");
    }
}
