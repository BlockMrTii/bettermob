package eu.northsoft.bettermob.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Reflection {
    private static final Map<Class<?>, Map<String, Method>> METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new ConcurrentHashMap<>();

    private Reflection() {}

    static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        Map<String, Method> methods = METHODS.computeIfAbsent(type, key -> new ConcurrentHashMap<>());
        StringBuilder key = new StringBuilder(name);
        for (Class<?> parameter : parameters) key.append('/').append(parameter.getName());
        Method method = methods.get(key.toString());
        if (method == null) {
            method = type.getMethod(name, parameters);
            methods.put(key.toString(), method);
        }
        return method;
    }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Map<String, Field> fields = FIELDS.computeIfAbsent(type, key -> new ConcurrentHashMap<>());
        Field field = fields.get(name);
        if (field == null) {
            field = type.getField(name);
            fields.put(name, field);
        }
        return field;
    }
}
