package eu.northsoft.bettermob.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ModelEngineApiTest {
    private static final String ME = "com.ticxo.modelengine.api.";

    private record Call(String type, String method, String... parameters) {}

    private static final List<Call> CALLS = List.of(
            new Call(ME + "ModelEngineAPI", "getBlueprint", "java.lang.String"),
            new Call(ME + "ModelEngineAPI", "getOrCreateModeledEntity", "org.bukkit.entity.Entity"),
            new Call(ME + "ModelEngineAPI", "createActiveModel", "java.lang.String"),
            new Call(ME + "model.ModeledEntity", "addModel", ME + "model.ActiveModel", "boolean"),
            new Call(ME + "model.ModeledEntity", "setBaseEntityVisible", "boolean"),
            new Call(ME + "model.ModeledEntity", "getBase"),
            new Call(ME + "model.ActiveModel", "destroy"),
            new Call(ME + "model.ActiveModel", "getAnimationHandler"),
            new Call(ME + "model.ActiveModel", "getBone", "java.lang.String"),
            new Call(ME + "model.ActiveModel", "getMountManager"),
            new Call(ME + "model.ActiveModel", "getModeledEntity"),
            new Call(ME + "animation.handler.AnimationHandler", "playAnimation", "java.lang.String", "double", "double", "double", "boolean"),
            new Call(ME + "model.bone.ModelBone", "getLocation"),
            new Call(ME + "model.bone.manager.MountManager", "getSeat", "java.lang.String"),
            new Call(ME + "model.bone.manager.MountManager", "mountDriver", "org.bukkit.entity.Entity", ME + "mount.controller.MountControllerSupplier"),
            new Call(ME + "model.bone.manager.MountManager", "mountPassenger", "java.lang.String", "org.bukkit.entity.Entity", ME + "mount.controller.MountControllerSupplier"),
            new Call(ME + "model.bone.type.Mount", "isDriver"),
            new Call(ME + "entity.BaseEntity", "getBodyRotationController"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setHeadClampUneven", "boolean"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setBodyClampUneven", "boolean"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setPlayerMode", "boolean"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setMinBodyAngle", "float"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setMaxBodyAngle", "float"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setMinHeadAngle", "float"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setMaxHeadAngle", "float"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setStableAngle", "float"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setRotationDuration", "int"),
            new Call(ME + "nms.entity.wrapper.BodyRotationController", "setRotationDelay", "int"));

    @Test
    void everyModelEngineCallTheHookMakesExistsInTheInstalledJar() throws Exception {
        String path = System.getProperty("modelengine.jar");
        assumeTrue(path != null && new File(path).isFile(), "set -Dmodelengine.jar=<ModelEngine jar> to run this check");
        List<String> missing = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{new File(path).toURI().toURL()}, getClass().getClassLoader())) {
            for (Call call : CALLS) {
                try {
                    Class<?>[] parameters = new Class<?>[call.parameters().length];
                    for (int i = 0; i < parameters.length; i++) parameters[i] = type(call.parameters()[i], loader);
                    Class.forName(call.type(), false, loader).getMethod(call.method(), parameters);
                } catch (ReflectiveOperationException exception) {
                    missing.add(call.type() + "#" + call.method() + " " + exception);
                }
            }
            Class<?> types = Class.forName(ME + "mount.controller.MountControllerTypes", false, loader);
            for (String field : List.of("WALKING", "FLYING", "WALKING_FORCE", "FLYING_FORCE")) {
                try {
                    types.getField(field);
                } catch (NoSuchFieldException exception) {
                    missing.add("MountControllerTypes." + field);
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> String.join("\n", missing));
    }

    @Test
    void controllerNamesMapToTheModelEngineFields() {
        assertEquals("WALKING", ModelEngineHook.controllerField(null));
        assertEquals("WALKING", ModelEngineHook.controllerField("walking"));
        assertEquals("FLYING", ModelEngineHook.controllerField("Flying"));
        assertEquals("FLYING_FORCE", ModelEngineHook.controllerField("flying-force"));
        assertEquals("WALKING_FORCE", ModelEngineHook.controllerField("walking_force"));
        assertEquals("WALKING", ModelEngineHook.controllerField("unknown"));
    }

    private static Class<?> type(String name, ClassLoader loader) throws ClassNotFoundException {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "int" -> int.class;
            default -> Class.forName(name, false, loader);
        };
    }
}
