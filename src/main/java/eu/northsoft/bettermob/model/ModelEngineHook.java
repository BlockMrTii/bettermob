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

    private volatile Method getOrCreateModeledEntityMethod;
    private volatile Method createActiveModelMethod;
    private volatile Method addModelMethod;
    private volatile Method destroyMethod;

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

    private static final String MODEL_BONE = "com.ticxo.modelengine.api.model.bone.ModelBone";
    private static final String ANIMATION_PROPERTY = "com.ticxo.modelengine.api.animation.property.IAnimationProperty";
    private static final String LOOP_MODE = "com.ticxo.modelengine.api.animation.BlueprintAnimation$LoopMode";
    private static final String DEFAULT_PROPERTY = "com.ticxo.modelengine.api.animation.handler.AnimationHandler$DefaultProperty";
    private static final String MODEL_STATE = "com.ticxo.modelengine.api.animation.ModelState";

    private interface BoneCall {
        void apply(Class<?> boneType, Object bone) throws ReflectiveOperationException;
    }

    private boolean eachBone(Object activeModel, String action, String part, boolean exact, BoneCall call) {
        if (activeModel == null) return false;
        try {
            Class<?> boneType = Class.forName(MODEL_BONE);
            Object bones = Reflection.method(Class.forName(ACTIVE_MODEL), "getBones").invoke(activeModel);
            boolean any = false;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) bones).entrySet()) {
                if (!ModelParts.matches(String.valueOf(entry.getKey()), part, exact)) continue;
                call.apply(boneType, entry.getValue());
                any = true;
            }
            if (!any && part != null && !part.isBlank()) plugin.messages().warn("modelEngine.boneMissing", "bone", part);
            return any;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", action, "error", rootMessage(exception));
            return false;
        }
    }

    private boolean onModel(Object activeModel, String action, BoneCall call) {
        if (activeModel == null) return false;
        try {
            call.apply(Class.forName(ACTIVE_MODEL), activeModel);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.messages().warn("modelEngine.callFailed", "action", action, "error", rootMessage(exception));
            return false;
        }
    }

    public boolean tint(Object activeModel, String part, boolean exact, int rgb) {
        org.bukkit.Color color = org.bukkit.Color.fromRGB(rgb & 0xFFFFFF);
        if (part == null || part.isBlank()) {
            return onModel(activeModel, "tint", (type, model) -> Reflection.method(type, "setDefaultTint", org.bukkit.Color.class).invoke(model, color));
        }
        return eachBone(activeModel, "tint", part, exact, (type, bone) -> Reflection.method(type, "setDefaultTint", org.bukkit.Color.class).invoke(bone, color));
    }

    public boolean glow(Object activeModel, String part, boolean exact, boolean glowing, int rgb) {
        BoneCall call = (type, target) -> {
            Reflection.method(type, "setGlowing", Boolean.class).invoke(target, glowing);
            Reflection.method(type, "setGlowColor", Integer.class).invoke(target, rgb & 0xFFFFFF);
        };
        if (part == null || part.isBlank()) return onModel(activeModel, "glow", call);
        return eachBone(activeModel, "glow", part, exact, call);
    }

    public boolean partVisible(Object activeModel, String part, boolean exact, boolean visible) {
        return eachBone(activeModel, "partvis", part, exact, (type, bone) -> Reflection.method(type, "setVisible", boolean.class).invoke(bone, visible));
    }

    public boolean setItem(Object activeModel, String part, boolean exact, org.bukkit.inventory.ItemStack item) {
        org.bukkit.inventory.ItemStack stack = item == null ? new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR) : item;
        return eachBone(activeModel, "setitemmodel", part, exact, (type, bone) -> Reflection.method(type, "setModel", org.bukkit.inventory.ItemStack.class).invoke(bone, stack));
    }

    public boolean lockRotation(Object activeModel, boolean locked) {
        return onModel(activeModel, "lockmodel", (type, model) -> {
            Object modeled = Reflection.method(type, "getModeledEntity").invoke(model);
            if (modeled != null) Reflection.method(Class.forName(MODELED_ENTITY), "setModelRotationLocked", boolean.class).invoke(modeled, locked);
        });
    }

    public boolean modifyAnimation(Object activeModel, String animation, Float speed, String loop) {
        return onModel(activeModel, "modifystate " + animation, (type, model) -> {
            Object handler = Reflection.method(type, "getAnimationHandler").invoke(model);
            Object property = Reflection.method(Class.forName(ANIMATION_HANDLER), "getAnimation", String.class).invoke(handler, animation);
            if (property == null) return;
            Class<?> propertyType = Class.forName(ANIMATION_PROPERTY);
            if (speed != null) Reflection.method(propertyType, "setSpeed", double.class).invoke(property, speed.doubleValue());
            if (loop != null) {
                Class<?> loopType = Class.forName(LOOP_MODE);
                Object mode = Reflection.method(loopType, "getOrNull", String.class).invoke(null, loop.trim().toUpperCase(Locale.ROOT));
                if (mode != null) Reflection.method(propertyType, "setForceLoopMode", loopType).invoke(property, mode);
            }
        });
    }

    public boolean defaultState(Object activeModel, String state, String animation, double lerpIn, double lerpOut, double speed) {
        return onModel(activeModel, "defaultstate " + state, (type, model) -> {
            Class<?> stateType = Class.forName(MODEL_STATE);
            Object modelState = Reflection.field(stateType, state.trim().toUpperCase(Locale.ROOT)).get(null);
            Object property = Class.forName(DEFAULT_PROPERTY).getConstructor(stateType, String.class, double.class, double.class, double.class)
                    .newInstance(modelState, animation, lerpIn, lerpOut, speed);
            Object handler = Reflection.method(type, "getAnimationHandler").invoke(model);
            Reflection.method(Class.forName(ANIMATION_HANDLER), "setDefaultProperty", Class.forName(DEFAULT_PROPERTY)).invoke(handler, property);
        });
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
