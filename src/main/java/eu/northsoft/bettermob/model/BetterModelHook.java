package eu.northsoft.bettermob.model;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Map;

public final class BetterModelHook {
    private static final String API = "kr.toxicity.model.api.BetterModel";
    private static final String ADAPTER = "kr.toxicity.model.api.bukkit.platform.BukkitAdapter";
    private static final String PLATFORM_ENTITY = "kr.toxicity.model.api.platform.PlatformEntity";
    private static final String MODIFIER = "kr.toxicity.model.api.animation.AnimationModifier";
    private static final String ITERATOR_TYPE = "kr.toxicity.model.api.animation.AnimationIterator$Type";

    private final BetterMobPlugin plugin;

    private volatile Method modelOrNullMethod;
    private volatile Method adaptMethod;
    private volatile Class<?> platformEntityClass;
    private volatile Object playOnceModifier;
    private volatile Method animateMethod;
    private volatile Class<?> modifierClass;

    public BetterModelHook(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("BetterModel") && classExists(API);
    }

    public boolean hasModel(String modelId) {
        if (!available()) return false;
        try {
            return modelOrNullMethod().invoke(null, modelId) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    public Object attachIfPresent(Entity entity, String modelId) {
        if (!available()) return null;
        try {
            if (modelOrNullMethod().invoke(null, modelId) == null) return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        return attach(entity, modelId);
    }

    public Object attach(Entity entity, String modelId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
            plugin.messages().warn("betterModel.inactive", "model", modelId);
            return null;
        }
        if (!classExists(API)) {
            plugin.messages().warn("betterModel.apiClassMissing", "class", API);
            return null;
        }
        try {
            Object renderer = modelOrNullMethod().invoke(null, modelId);
            if (renderer == null) {
                plugin.messages().warn("betterModel.modelMissing", "model", modelId);
                return null;
            }
            Object adapted = adaptMethod().invoke(null, entity);
            return renderer.getClass().getMethod("getOrCreate", platformEntityClass()).invoke(renderer, adapted);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.messages().warn("betterModel.attachFailed", "model", modelId, "error", rootMessage(exception));
            return null;
        }
    }

    public boolean play(Object tracker, String animation) {
        if (tracker == null || !available()) return false;
        try {
            Object modifier = playOnceModifier();
            Method method = animateMethod(tracker);
            Object result;
            try {
                result = method.invoke(tracker, animation, modifier);
            } catch (IllegalArgumentException mismatch) {
                method = tracker.getClass().getMethod("animate", String.class, modifierClass());
                result = method.invoke(tracker, animation, modifier);
            }
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.messages().warn("betterModel.animationFailed", "animation", animation, "error", rootMessage(exception));
            return false;
        }
    }

    private Method modelOrNullMethod() throws ReflectiveOperationException {
        if (modelOrNullMethod == null) modelOrNullMethod = Class.forName(API).getMethod("modelOrNull", String.class);
        return modelOrNullMethod;
    }

    private Method adaptMethod() throws ReflectiveOperationException {
        if (adaptMethod == null) adaptMethod = Class.forName(ADAPTER).getMethod("adapt", Entity.class);
        return adaptMethod;
    }

    private Class<?> platformEntityClass() throws ReflectiveOperationException {
        if (platformEntityClass == null) platformEntityClass = Class.forName(PLATFORM_ENTITY);
        return platformEntityClass;
    }

    private Class<?> modifierClass() throws ReflectiveOperationException {
        if (modifierClass == null) modifierClass = Class.forName(MODIFIER);
        return modifierClass;
    }

    private Object playOnceModifier() throws ReflectiveOperationException {
        if (playOnceModifier != null) return playOnceModifier;
        Class<?> modifier = modifierClass();
        Class<?> typeClass = Class.forName(ITERATOR_TYPE);
        Object builder = modifier.getMethod("builder").invoke(null);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object type = Enum.valueOf((Class<? extends Enum>) typeClass, "PLAY_ONCE");
        builder.getClass().getMethod("type", typeClass).invoke(builder, type);
        playOnceModifier = builder.getClass().getMethod("build").invoke(builder);
        return playOnceModifier;
    }

    private Method animateMethod(Object tracker) throws ReflectiveOperationException {
        if (animateMethod == null) animateMethod = tracker.getClass().getMethod("animate", String.class, modifierClass());
        return animateMethod;
    }

    private final Map<Class<?>, Map<String, Method>> methodCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, java.lang.reflect.Field>> fieldCache = new java.util.concurrent.ConcurrentHashMap<>();

    private Method cachedMethod(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        Map<String, Method> methods = methodCache.computeIfAbsent(type, key -> new java.util.concurrent.ConcurrentHashMap<>());
        String key = name + "/" + parameters.length;
        Method method = methods.get(key);
        if (method == null) {
            method = type.getMethod(name, parameters);
            methods.put(key, method);
        }
        return method;
    }

    private java.lang.reflect.Field cachedField(Class<?> type, String name) throws NoSuchFieldException {
        Map<String, java.lang.reflect.Field> fields = fieldCache.computeIfAbsent(type, key -> new java.util.concurrent.ConcurrentHashMap<>());
        java.lang.reflect.Field field = fields.get(name);
        if (field == null) {
            field = type.getField(name);
            fields.put(name, field);
        }
        return field;
    }

    public org.bukkit.Location bonePosition(Object tracker, String boneName, org.bukkit.Location origin) {
        if (tracker == null) return null;
        try {
            Object bone = cachedMethod(tracker.getClass(), "bone", String.class).invoke(tracker, boneName);
            if (bone == null) {
                plugin.messages().warn("betterModel.boneMissing", "bone", boneName);
                return null;
            }
            Object offset = cachedMethod(bone.getClass(), "worldPosition").invoke(bone);
            Class<?> vector = offset.getClass();
            double x = ((Number) cachedField(vector, "x").get(offset)).doubleValue();
            double y = ((Number) cachedField(vector, "y").get(offset)).doubleValue();
            double z = ((Number) cachedField(vector, "z").get(offset)).doubleValue();
            if (plugin.debug().verbose()) plugin.debug().verbose("bone '" + boneName + "' offset " + String.format("%.2f %.2f %.2f", x, y, z));
            return origin.clone().add(x, y, z);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.messages().warn("betterModel.modelPartFailed", "bone", boneName, "error", exception);
            return null;
        }
    }

    private static final Map<String, String> ROTATION_SETTERS = Map.of(
            "headuneven", "setHeadUneven", "bodyuneven", "setBodyUneven", "playermode", "setPlayerMode",
            "minbody", "setMinBody", "maxbody", "setMaxBody", "minhead", "setMinHead", "maxhead", "setMaxHead",
            "stable", "setStable", "duration", "setRotationDuration", "delay", "setRotationDelay");

    public boolean bodyRotation(Object tracker, Map<String, String> params) {
        if (tracker == null) return false;
        try {
            Object rotator = cachedMethod(tracker.getClass(), "bodyRotator").invoke(tracker);
            Method setValue = cachedMethod(rotator.getClass(), "setValue", java.util.function.Consumer.class);
            setValue.invoke(rotator, (java.util.function.Consumer<Object>) data -> {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String setter = ROTATION_SETTERS.get(entry.getKey());
                    if (setter != null) applySetter(data, setter, entry.getValue());
                }
            });
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.messages().warn("betterModel.bodyRotationFailed", "error", exception);
            return false;
        }
    }

    private final Map<Class<?>, Map<String, Method>> setterCache = new java.util.concurrent.ConcurrentHashMap<>();

    private Method setterOf(Class<?> type, String name) {
        return setterCache.computeIfAbsent(type, key -> new java.util.concurrent.ConcurrentHashMap<>()).computeIfAbsent(name, key -> {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 1) return method;
            }
            return null;
        });
    }

    private void applySetter(Object data, String name, String value) {
        Method method = setterOf(data.getClass(), name);
        if (method == null) return;
        Class<?> type = method.getParameterTypes()[0];
        try {
            Object parsed = type == boolean.class ? Boolean.parseBoolean(value)
                    : type == int.class ? (Object) Integer.parseInt(value.trim())
                    : (Object) Float.parseFloat(value.trim());
            method.invoke(data, parsed);
        } catch (ReflectiveOperationException | NumberFormatException exception) {
            plugin.messages().warn("betterModel.bodyRotationInvalid", "value", value, "name", name);
        }
    }

    public boolean mount(Object tracker, String seat, Entity rider) {
        if (tracker == null) return false;
        try {
            Object bone = cachedMethod(tracker.getClass(), "bone", String.class).invoke(tracker, seat);
            if (bone == null) {
                plugin.messages().warn("betterModel.mount.seatMissing", "seat", seat);
                return false;
            }
            Object hitBox = cachedMethod(bone.getClass(), "getHitBox").invoke(bone);
            if (hitBox == null) {
                plugin.messages().warn("betterModel.mount.noHitbox", "seat", seat);
                return false;
            }
            allowControl(hitBox);

            Object adapted = adaptMethod().invoke(null, rider);
            Method mountMethod = findMethod(hitBox.getClass(), "mount", 1);
            if (mountMethod == null) {
                plugin.messages().warn("betterModel.mount.noMountMethod", "class", hitBox.getClass().getName());
                return false;
            }
            mountMethod.invoke(hitBox, adapted);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.messages().warn("betterModel.mount.seatFailed", "seat", seat, "error", rootMessage(exception));
            return false;
        }
    }

    private void allowControl(Object hitBox) {
        Method method = findMethod(hitBox.getClass(), "mountController", 1);
        if (method == null) return;
        Object control = resolveConstant(method.getParameterTypes()[0], "CONTROL");
        if (control == null) {
            plugin.messages().warn("betterModel.mount.controlConstantMissing", "type", method.getParameterTypes()[0].getName());
            return;
        }
        try {
            method.invoke(hitBox, control);
        } catch (ReflectiveOperationException exception) {
            plugin.messages().warn("betterModel.mount.controlModeFailed", "error", rootMessage(exception));
        }
    }

    private static Object resolveConstant(Class<?> type, String name) {
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) if (constant.toString().equalsIgnoreCase(name)) return constant;
        }
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            return Class.forName(type.getName() + "s").getField(name).get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) return method;
        }
        return null;
    }

    public void close(Object tracker) {
        if (tracker == null) return;
        try {
            tracker.getClass().getMethod("close").invoke(tracker);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
