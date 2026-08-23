package cn.jowen.framework.extras.desensitize.serializer;

import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.module.SimpleModule;

/**
 * 脱敏 Jackson 模块：向 {@link tools.jackson.databind.json.JsonMapper} 注册
 * {@link DesensitizeJsonSerializer}，使标注脱敏注解的字符串字段在序列化时自动脱敏。
 *
 * <p>用法：
 * <pre>{@code
 * JsonMapper mapper = JsonMapper.builder().addModule(new DesensitizeModule()).build();
 * String json = mapper.writeValueAsString(userVO);   // @PhoneDesensitize 字段 -> 138****5678
 * }</pre>
 *
 * <p>本模块属于可选能力（依赖 {@code tools.jackson.core:jackson-databind}），
 * 在 {@code framework-extras} 中以 {@code <optional>true</optional>} 引入，不强制业务方依赖 Jackson。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class DesensitizeModule extends SimpleModule {

    /**
     * 模块名称。
     */
    public static final String MODULE_NAME = "JowenDesensitizeModule";
    private static final long serialVersionUID = 1L;

    public DesensitizeModule() {
        super(MODULE_NAME);
        // 主路径：所有 String 类型走脱敏序列化器（无注解字段透传）
        addSerializer(String.class, new DesensitizeJsonSerializer());
        // 冗余保障：序列化器构建阶段同样将 String 替换为脱敏序列化器
        setSerializerModifier(new DesensitizeSerializerModifier());
    }
}
