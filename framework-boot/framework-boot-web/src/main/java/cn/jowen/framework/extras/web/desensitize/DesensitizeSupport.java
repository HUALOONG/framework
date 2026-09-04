package cn.jowen.framework.extras.web.desensitize;

import cn.jowen.framework.core.desensitize.DesensitizeStrategies;
import cn.jowen.framework.core.desensitize.Desensitizer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 脱敏支撑器：把核心层 {@link Desensitizer} 的 POJO 脱敏能力
 * 扩展到 Web 返回值的常见形态（单对象 / List / 数组 / Map）。
 *
 * <p>处理规则：
 * <ul>
 *   <li>{@code null} 原样返回；</li>
 *   <li>数组与 {@code List}：逐元素脱敏；不可变 List 复制为可变副本后原位改写，
 *       不修改调用方传入的原始集合语义；</li>
 *   <li>{@code Map}：值为 String 时按内置规则自动识别脱敏（委托
 *       {@link Desensitizer#maskMap(Map, String)}），其余值原样保留；</li>
 *   <li>其他对象：委托 {@link Desensitizer#maskObject(Object)}
 *       反射识别 {@code @DesensitizeField} 字段。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DesensitizeSupport {

    private DesensitizeSupport() {
    }

    /**
     * 对返回值执行脱敏。
     *
     * @param result 方法原始返回值，可为 {@code null}
     * @return 脱敏后的返回值；{@code null} 原样返回
     */
    public static @Nullable Object mask(@Nullable Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof List<?> list) {
            return maskList(list);
        }
        if (result.getClass().isArray()) {
            return maskArray(result);
        }
        if (result instanceof Map<?, ?> map) {
            return maskMap(map);
        }
        return Desensitizer.getInstance().maskObject(result);
    }

    /**
     * List 脱敏：不可变 List 复制为可变副本，逐元素委托
     * {@link Desensitizer#maskObject(Object)}。
     *
     * @param list 原始列表
     * @return 脱敏后的列表（可能为新副本）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<?> maskList(List<?> list) {
        List target = new ArrayList<>(list.size());
        for (Object element : list) {
            target.add(element == null ? null : Desensitizer.getInstance().maskObject(element));
        }
        return target;
    }

    /**
     * 数组脱敏：按元素类型创建同长度新数组，逐元素委托
     * {@link Desensitizer#maskObject(Object)}。
     *
     * @param array 原始数组
     * @return 脱敏后的新数组
     */
    private static Object maskArray(Object array) {
        int length = Array.getLength(array);
        Object masked = Array.newInstance(array.getClass().getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            Array.set(masked, i, element == null ? null : Desensitizer.getInstance().maskObject(element));
        }
        return masked;
    }

    /**
     * Map 脱敏：String 值按内置格式策略自动识别脱敏，非 String 值原样保留。
     *
     * <p>自动识别仅针对有格式约束的策略（手机号/身份证/银行卡/邮箱/座机/车牌），
     * 格式不符的值原样保留，避免误伤普通文本。
     *
     * @param map 原始 Map
     * @return 脱敏后的新 Map
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<?, ?> maskMap(Map<?, ?> map) {
        Map target = new java.util.LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            target.put(entry.getKey(), value instanceof String s ? maskAutoDetect(s) : value);
        }
        return target;
    }

    /**
     * 按候选策略自动识别并脱敏：依次尝试有格式约束的内置策略，
     * 命中第一个产生变化的策略即返回；全部未命中时原样返回。
     *
     * @param value 待识别文本
     * @return 脱敏后文本或原文
     */
    private static String maskAutoDetect(String value) {
        DesensitizeStrategies[] candidates = {
                DesensitizeStrategies.PHONE,
                DesensitizeStrategies.ID_CARD,
                DesensitizeStrategies.BANK_CARD,
                DesensitizeStrategies.EMAIL,
                DesensitizeStrategies.FIXED_PHONE,
                DesensitizeStrategies.LICENSE_PLATE,
        };
        Desensitizer desensitizer = Desensitizer.getInstance();
        for (DesensitizeStrategies strategy : candidates) {
            if (!strategy.matches(value)) {
                continue;
            }
            String masked = desensitizer.mask(value, strategy.name());
            if (!masked.equals(value)) {
                return masked;
            }
        }
        return value;
    }
}
