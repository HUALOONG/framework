package cn.jowen.framework.data.mybatis.extension;

import java.lang.reflect.Method;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FlexOptimisticLockHandler implements ExtensionRegistry.Extension {

    @Override public String name() { return "optimisticLock"; }
    @Override public int order() { return 400; }

    public void checkUpdateResult(Object entity, int rows) {
        if (rows == 0 && hasVersionField(entity)) {
            throw new cn.jowen.framework.data.mybatis.exception.FlexOptimisticLockException(
                    "乐观锁冲突: 实体 " + entity.getClass().getSimpleName() + " 版本字段冲突");
        }
    }

    public void incrementVersion(Object entity) {
        try {
            var getter = findMethod(entity.getClass(), "getVersion");
            getter.setAccessible(true);
            Object current = getter.invoke(entity);
            Object next;
            switch (current) {
                case Integer v -> next = v + 1;
                case Long v -> next = v + 1L;
                case null, default -> {
                    return;
                }
            }

            Class<?> setterType = next.getClass();
            java.lang.reflect.Method setter = findMethod(entity.getClass(), "setVersion", setterType);
            if (setter == null) {
                return;
            }
            setter.setAccessible(true);
            setter.invoke(entity, next);
        } catch (Exception ignored) {
            // No version field
        }
    }

    private boolean hasVersionField(Object entity) {
        try {
            return findMethod(entity.getClass(), "getVersion") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            Method m = clazz.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            // Try superclass
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                return findMethod(parent, name, paramTypes);
            }
            return null;
        }
    }
}
