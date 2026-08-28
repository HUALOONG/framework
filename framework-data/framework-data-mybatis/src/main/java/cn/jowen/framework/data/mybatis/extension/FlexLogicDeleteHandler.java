package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Method;

@NullMarked
/**
 * 「FlexLogicDelete」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexLogicDeleteHandler implements ExtensionRegistry.Extension {

    /** logger 常量。 */
    private static final Logger logger = LoggerFactory.getLogger(FlexLogicDeleteHandler.class);

    /** deleteField 字段。 */
    private String deleteField = "deleted";
    /** deletedValue 字段。 */
    private Object deletedValue = 1;

    /** public 字段。 */
    public FlexLogicDeleteHandler() {}
    /**
     * 构造实例。
     * @param deleteField 参数 deleteField
     * @param deletedValue 参数 deletedValue
     */
    public FlexLogicDeleteHandler(String deleteField, Object deletedValue) {
        this.deleteField = deleteField;
        this.deletedValue = deletedValue;
    }

    /** return 字段。 */
    @Override public String name() { return "logicDelete"; }
    /** return 字段。 */
    @Override public int order() { return 600; }

    /**
     * 转换为logic delete。
     * @param entity 参数 entity
     * @return 结果
     */
    public boolean toLogicDelete(Object entity) {
        try {
            String setterName = "set" + Character.toUpperCase(deleteField.charAt(0)) + deleteField.substring(1);
            Class<?> paramType = deletedValue.getClass();
            java.lang.reflect.Method setter = findDeclaredMethod(entity.getClass(), setterName, paramType);
            setter.setAccessible(true);
            setter.invoke(entity, deletedValue);
            return true;
        } catch (Exception e) {
            logger.debug("逻辑删除字段注入失败: " + e.getMessage());
            return false;
        }
    }

    private Method findDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                return findDeclaredMethod(parent, name, paramTypes);
            }
            return null;
        }
    }

    /** deleteField 字段。 */
    public String deleteCondition() { return deleteField + " = 0"; }

    /** deleteField 字段。 */
    public String getDeleteField() { return deleteField; }
    /** void 字段。 */
    public void setDeleteField(String v) { this.deleteField = v; }
    /** deletedValue 字段。 */
    public Object getDeletedValue() { return deletedValue; }
    /** void 字段。 */
    public void setDeletedValue(Object v) { this.deletedValue = v; }
}
