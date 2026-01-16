package main.java.optimizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class AstReflection {

    private AstReflection() {}

    static boolean trySetField(Object target, String fieldName, Object value) {
        if (target == null) return false;
        try {
            Field f = findField(target.getClass(), fieldName);
            if (f == null) return false;
            f.setAccessible(true);
            f.set(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Object tryGetField(Object target, String fieldName) {
        if (target == null) return null;
        try {
            Field f = findField(target.getClass(), fieldName);
            if (f == null) return null;
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Object tryCallGetter(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static <T> T tryConstruct(Class<T> cls, Object... args) {
        if (cls == null) return null;
        try {
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                if (pts.length != args.length) continue;
                if (!isCompatible(pts, args)) continue;
                c.setAccessible(true);
                @SuppressWarnings("unchecked")
                T obj = (T) c.newInstance(args);
                return obj;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isCompatible(Class<?>[] pts, Object[] args) {
        for (int i = 0; i < pts.length; i++) {
            if (args[i] == null) continue;
            Class<?> need = wrap(pts[i]);
            Class<?> got = wrap(args[i].getClass());
            if (!need.isAssignableFrom(got)) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == char.class) return Character.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        return c;
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }
}
