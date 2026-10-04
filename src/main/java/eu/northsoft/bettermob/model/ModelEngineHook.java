package eu.northsoft.bettermob.model;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModelEngineHook {
    private static final String API = "com.ticxo.modelengine.api.ModelEngineAPI";
    private static final String ACTIVE_MODEL = "com.ticxo.modelengine.api.model.ActiveModel";
    private static final String ANIMATION_HANDLER = "com.ticxo.modelengine.api.animation.handler.AnimationHandler";
    private static final String MODELED_ENTITY = "com.ticxo.modelengine.api.model.ModeledEntity";
    private static final String BASE_ENTITY = "com.ticxo.modelengine.api.entity.BaseEntity";
    private static final String BODY_ROTATION = "com.ticxo.modelengine.api.nms.entity.wrapper.BodyRotationController";
    private static final String MOUNT_MANAGER = "com.ticxo.modelengine.api.model.bone.manager.MountManager";
    private static final String CONTROLLER_SUPPLIER = "com.ticxo.modelengine.api.mount.controller.MountControllerSupplier";
    private static final String CONTROLLER_TYPES = "com.ticxo.modelengine.api.mount.controller.MountControllerTypes";
    private static final Map<String, String> ROTATION_SETTERS = Map.of(
            "headuneven", "setHeadClampUneven", "bodyuneven", "setBodyClampUneven", "playermode", "setPlayerMode",
            "minbody", "setMinBodyAngle", "maxbody", "setMaxBodyAngle", "minhead", "setMinHeadAngle", "maxhead", "setMaxHeadAngle",
            "stable", "setStableAngle", "duration", "setRotationDuration", "delay", "setRotationDelay");

    private final BetterMobPlugin plugin;

    private Method getOrCreateModeledEntityMethod;
    private Method createActiveModelMethod;
    private Method addModelMethod;
    private Method destroyMethod;

    public ModelEngineHook(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("ModelEngine") && classExists(API);
    }

    public Object attach(Entity entity, String modelId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            plugin.messages().warn("modelEngine.inactive", "model", modelId);
            return null;
        }
        if (!classExists(API)) {
            plugin.messages().warn("modelEngine.apiClassMissing", "class", API);
            return null;
        }
        try {
            Object modeledEntity = getOrCreateModeledEntityMethod().invoke(null, entity);
            Object activeModel = createActiveModelMethod().invoke(null, modelId);
            Object result = addModelMethod(modeledEntity).invoke(modeledEntity, activeModel, true);
            if (result instanceof Optional<?> optional && optional.isEmpty()) {
                plugin.messages().warn("modelEngine.addFailed", "model", modelId);
                return null;
            }

            trySetBaseEntityInvisible(modeledEntity);
            return activeModel;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.messages().warn("modelEngine.attachFailed", "model", modelId, "error", rootMessage(exception));
            return null;
        }
    }

    public boolean hasModel(String modelId) {
        if (!available()) return false;
        try {
            return Reflection.method(Class.forName(API), "getBlueprint", String.class).invoke(null, modelId) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    public Object attachIfPresent(Entity entity, String modelId) {
        return hasModel(modelId) ? attach(entity, modelId) : null;
    }

    public boolean play(Object activeModel, String animation, Map<String, String> params) {
        if (activeModel == null) return false;
        try {
            Object handler = Reflection.method(Class.forName(ACTIVE_MODEL), "getAnimationHandler").invoke(activeModel);
            double lerpIn = number(params, 0.1, "lerpin", "li");
            double lerpOut = number(params, 0.1, "lerpout", "lo");
            double speed = number(params, 1.0, "speed", "sp");
            boolean force = !"false".equalsIgnoreCase(params.get("force"));
            Reflection.method(Class.forName(ANIMATION_HANDLER), "playAnimation", String.class, double.class, double.class, double.class, boolean.class)
                    .invoke(handler, animation, lerpIn, lerpOut, speed, force);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", "animation " + animation, "error", rootMessage(exception));
            return false;
        }
    }

    public Location bonePosition(Object activeModel, String boneName) {
        if (activeModel == null || boneName == null) return null;
        try {
            Object bone = optional(Reflection.method(Class.forName(ACTIVE_MODEL), "getBone", String.class).invoke(activeModel, boneName));
            if (bone == null) {
                plugin.messages().warn("modelEngine.boneMissing", "bone", boneName);
                return null;
            }
            Object location = Reflection.method(Class.forName("com.ticxo.modelengine.api.model.bone.ModelBone"), "getLocation").invoke(bone);
            return location instanceof Location result ? result.clone() : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", "bone " + boneName, "error", rootMessage(exception));
            return null;
        }
    }

    public boolean mount(Object activeModel, String seat, Entity rider, String controller) {
        if (activeModel == null) return false;
        try {
            Object manager = optional(Reflection.method(Class.forName(ACTIVE_MODEL), "getMountManager").invoke(activeModel));
            if (manager == null) {
                plugin.messages().warn("modelEngine.mount.noMountBones");
                return false;
            }
            Class<?> managerType = Class.forName(MOUNT_MANAGER);
            Class<?> supplierType = Class.forName(CONTROLLER_SUPPLIER);
            Object supplier = Reflection.field(Class.forName(CONTROLLER_TYPES), controllerField(controller)).get(null);
            Object found = optional(Reflection.method(managerType, "getSeat", String.class).invoke(manager, seat));
            boolean mounted;
            if (found != null && !(Boolean) Reflection.method(Class.forName("com.ticxo.modelengine.api.model.bone.type.Mount"), "isDriver").invoke(found)) {
                mounted = (Boolean) Reflection.method(managerType, "mountPassenger", String.class, Entity.class, supplierType).invoke(manager, seat, rider, supplier);
            } else {
                mounted = (Boolean) Reflection.method(managerType, "mountDriver", Entity.class, supplierType).invoke(manager, rider, supplier);
            }
            if (!mounted) plugin.messages().warn("modelEngine.mount.failed", "seat", seat);
            return mounted;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", "mount " + seat, "error", rootMessage(exception));
            return false;
        }
    }

    public boolean bodyRotation(Object activeModel, Map<String, String> params) {
        if (activeModel == null) return false;
        try {
            Object modeled = Reflection.method(Class.forName(ACTIVE_MODEL), "getModeledEntity").invoke(activeModel);
            if (modeled == null) return false;
            Object base = Reflection.method(Class.forName(MODELED_ENTITY), "getBase").invoke(modeled);
            Object rotation = Reflection.method(Class.forName(BASE_ENTITY), "getBodyRotationController").invoke(base);
            Class<?> rotationType = Class.forName(BODY_ROTATION);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String setter = ROTATION_SETTERS.get(entry.getKey());
                if (setter != null) applyRotation(rotation, rotationType, setter, entry.getValue());
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", "body rotation", "error", rootMessage(exception));
            return false;
        }
    }

    private void applyRotation(Object rotation, Class<?> type, String setter, String value) throws ReflectiveOperationException {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(setter) || method.getParameterCount() != 1) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            try {
                Object parsed = parameter == boolean.class ? Boolean.parseBoolean(value)
                        : parameter == int.class ? (Object) Integer.parseInt(value.trim())
                        : (Object) Float.parseFloat(value.trim());
                method.invoke(rotation, parsed);
            } catch (NumberFormatException exception) {
                plugin.messages().warn("betterModel.bodyRotationInvalid", "value", value, "name", setter);
            }
            return;
        }
    }

    static String controllerField(String name) {
        if (name == null) return "WALKING";
        return switch (name.trim().toLowerCase(Locale.ROOT).replace("-", "_")) {
            case "flying" -> "FLYING";
            case "walking_force" -> "WALKING_FORCE";
            case "flying_force" -> "FLYING_FORCE";
            default -> "WALKING";
        };
    }

    private static Object optional(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private static double number(Map<String, String> params, double fallback, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (value == null) continue;
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
        return fallback;
    }

    public void close(Object tracker) {
        if (tracker == null) return;
        try {
            if (destroyMethod == null) destroyMethod = tracker.getClass().getMethod("destroy");
            destroyMethod.invoke(tracker);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void trySetBaseEntityInvisible(Object modeledEntity) {
        try {
            modeledEntity.getClass().getMethod("setBaseEntityVisible", boolean.class).invoke(modeledEntity, false);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Method getOrCreateModeledEntityMethod() throws ReflectiveOperationException {
        if (getOrCreateModeledEntityMethod == null) {
            getOrCreateModeledEntityMethod = Class.forName(API).getMethod("getOrCreateModeledEntity", Entity.class);
        }
        return getOrCreateModeledEntityMethod;
    }

    private Method createActiveModelMethod() throws ReflectiveOperationException {
        if (createActiveModelMethod == null) {
            createActiveModelMethod = Class.forName(API).getMethod("createActiveModel", String.class);
        }
        return createActiveModelMethod;
    }

    private Method addModelMethod(Object modeledEntity) throws ReflectiveOperationException {
        if (addModelMethod == null) {
            Class<?> activeModelClass = Class.forName(ACTIVE_MODEL);
            addModelMethod = modeledEntity.getClass().getMethod("addModel", activeModelClass, boolean.class);
        }
        return addModelMethod;
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
