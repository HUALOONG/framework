package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Method;

@NullMarked
public class FlexLogicDeleteHandler implements ExtensionRegistry.Extension {

    private static final Logger logger = LoggerFactory.getLogger(FlexLogicDeleteHandler.class);

    private String deleteField = "deleted";
    private Object deletedValue = 1;

    public FlexLogicDeleteHandler() {}
    public FlexLogicDeleteHandler(String deleteField, Object deletedValue) {
        this.deleteField = deleteField;
        this.deletedValue = deletedValue;
    }

    @Override public String name() { return "logicDelete"; }
    @Override public int order() { return 600; }

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

    public String deleteCondition() { return deleteField + " = 0"; }

    public String getDeleteField() { return deleteField; }
    public void setDeleteField(String v) { this.deleteField = v; }
    public Object getDeletedValue() { return deletedValue; }
    public void setDeletedValue(Object v) { this.deletedValue = v; }
}
