package eu.northsoft.bettermob;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Map;

final class BetterModelHook {
    private static final String API = "kr.toxicity.model.api.BetterModel";
    private static final String ADAPTER = "kr.toxicity.model.api.bukkit.platform.BukkitAdapter";
    private static final String PLATFORM_ENTITY = "kr.toxicity.model.api.platform.PlatformEntity";
    private static final String MODIFIER = "kr.toxicity.model.api.animation.AnimationModifier";
    private static final String ITERATOR_TYPE = "kr.toxicity.model.api.animation.AnimationIterator$Type";

    private final BetterMobPlugin plugin;

    private Method modelOrNullMethod;
    private Method adaptMethod;
    private Class<?> platformEntityClass;
    private Object playOnceModifier;
    private Method animateMethod;
    private Class<?> modifierClass;

    BetterModelHook(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("BetterModel") && classExists(API);
    }

    Object attachIfPresent(Entity entity, String modelId) {
        if (!available()) return null;
        try {
            if (modelOrNullMethod().invoke(null, modelId) == null) return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        return attach(entity, modelId);
    }

    Object attach(Entity entity, String modelId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
            plugin.getLogger().warning("BetterModel ist beim Spawn von '" + modelId + "' nicht aktiv - kein Modell angehaengt.");
            return null;
        }
        if (!classExists(API)) {
            plugin.getLogger().warning("BetterModel-API-Klasse '" + API + "' nicht gefunden - passt die installierte BetterModel-Version?");
            return null;
        }
        try {
            Object renderer = modelOrNullMethod().invoke(null, modelId);
            if (renderer == null) {
                plugin.getLogger().warning("BetterModel-Modell '" + modelId + "' existiert nicht.");
                return null;
            }
            Object adapted = adaptMethod().invoke(null, entity);
            return renderer.getClass().getMethod("getOrCreate", platformEntityClass()).invoke(renderer, adapted);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("BetterModel '" + modelId + "' konnte nicht angehängt werden: " + rootMessage(exception));
            return null;
        }
    }

    boolean play(Object tracker, String animation) {
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
            plugin.getLogger().warning("BetterModel-Animation '" + animation + "' fehlgeschlagen: " + rootMessage(exception));
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

    org.bukkit.Location bonePosition(Object tracker, String boneName, org.bukkit.Location origin) {
        if (tracker == null) return null;
        try {
            Object bone = tracker.getClass().getMethod("bone", String.class).invoke(tracker, boneName);
            if (bone == null) {
                plugin.getLogger().warning("@ModelPart: Bone '" + boneName + "' existiert nicht am Modell.");
                return null;
            }
            Object offset = bone.getClass().getMethod("worldPosition").invoke(bone);
            Class<?> vector = offset.getClass();
            double x = ((Number) vector.getField("x").get(offset)).doubleValue();
            double y = ((Number) vector.getField("y").get(offset)).doubleValue();
            double z = ((Number) vector.getField("z").get(offset)).doubleValue();
            return origin.clone().add(x, y, z);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("@ModelPart '" + boneName + "' fehlgeschlagen: " + exception);
            return null;
        }
    }

    private static final Map<String, String> ROTATION_SETTERS = Map.of(
            "headuneven", "setHeadUneven", "bodyuneven", "setBodyUneven", "playermode", "setPlayerMode",
            "minbody", "setMinBody", "maxbody", "setMaxBody", "minhead", "setMinHead", "maxhead", "setMaxHead",
            "stable", "setStable", "duration", "setRotationDuration", "delay", "setRotationDelay");

    boolean bodyRotation(Object tracker, Map<String, String> params) {
        if (tracker == null) return false;
        try {
            Object rotator = tracker.getClass().getMethod("bodyRotator").invoke(tracker);
            Method setValue = rotator.getClass().getMethod("setValue", java.util.function.Consumer.class);
            setValue.invoke(rotator, (java.util.function.Consumer<Object>) data -> {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String setter = ROTATION_SETTERS.get(entry.getKey());
                    if (setter != null) applySetter(data, setter, entry.getValue());
                }
            });
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("BodyRotation fehlgeschlagen: " + exception);
            return false;
        }
    }

    private void applySetter(Object data, String name, String value) {
        for (Method method : data.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            Class<?> type = method.getParameterTypes()[0];
            try {
                Object parsed = type == boolean.class ? Boolean.parseBoolean(value)
                        : type == int.class ? (Object) Integer.parseInt(value.trim())
                        : (Object) Float.parseFloat(value.trim());
                method.invoke(data, parsed);
            } catch (ReflectiveOperationException | NumberFormatException exception) {
                plugin.getLogger().warning("BodyRotation: '" + value + "' ist kein gueltiger Wert fuer " + name + ".");
            }
            return;
        }
    }

    boolean mount(Object tracker, String seat, Entity rider) {
        if (tracker == null) return false;
        try {
            Object bone = tracker.getClass().getMethod("bone", String.class).invoke(tracker, seat);
            if (bone == null) {
                plugin.getLogger().warning("mountmodel: Bone/Sitz '" + seat + "' existiert nicht an diesem Modell.");
                return false;
            }
            Object hitBox = bone.getClass().getMethod("getHitBox").invoke(bone);
            if (hitBox == null) {
                plugin.getLogger().warning("mountmodel: Bone '" + seat + "' hat keine Hitbox zum Draufsitzen.");
                return false;
            }
            allowControl(hitBox);

            Object adapted = adaptMethod().invoke(null, rider);
            Method mountMethod = findMethod(hitBox.getClass(), "mount", 1);
            if (mountMethod == null) {
                plugin.getLogger().warning("mountmodel: HitBox-Klasse '" + hitBox.getClass().getName() + "' hat keine mount(...)-Methode.");
                return false;
            }
            mountMethod.invoke(hitBox, adapted);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("mountmodel Sitz '" + seat + "' fehlgeschlagen: " + rootMessage(exception));
            return false;
        }
    }

    private void allowControl(Object hitBox) {
        Method method = findMethod(hitBox.getClass(), "mountController", 1);
        if (method == null) return;
        Object control = resolveConstant(method.getParameterTypes()[0], "CONTROL");
        if (control == null) {
            plugin.getLogger().warning("mountmodel: Steuerungs-Konstante 'CONTROL' fuer " + method.getParameterTypes()[0].getName() + " nicht gefunden - Sitz bleibt ohne Lenkung.");
            return;
        }
        try {
            method.invoke(hitBox, control);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("mountmodel: Steuerungsmodus konnte nicht gesetzt werden: " + rootMessage(exception));
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

    void close(Object tracker) {
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
