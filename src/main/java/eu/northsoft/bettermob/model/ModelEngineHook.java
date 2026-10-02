package eu.northsoft.bettermob.model;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ModelEngineHook {
    private static final String API = "com.ticxo.modelengine.api.ModelEngineAPI";
    private static final String ACTIVE_MODEL = "com.ticxo.modelengine.api.model.ActiveModel";

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
            plugin.getLogger().warning("ModelEngine ist beim Spawn von '" + modelId + "' nicht aktiv - kein Modell angehaengt.");
            return null;
        }
        if (!classExists(API)) {
            plugin.getLogger().warning("ModelEngine-API-Klasse '" + API + "' nicht gefunden - passt die installierte ModelEngine-Version?");
            return null;
        }
        try {
            Object modeledEntity = getOrCreateModeledEntityMethod().invoke(null, entity);
            Object activeModel = createActiveModelMethod().invoke(null, modelId);
            Object result = addModelMethod(modeledEntity).invoke(modeledEntity, activeModel, true);
            if (result instanceof Optional<?> optional && optional.isEmpty()) {
                plugin.getLogger().warning("ModelEngine-Modell '" + modelId + "' konnte nicht hinzugefuegt werden (existiert die Model-ID?).");
                return null;
            }

            trySetBaseEntityInvisible(modeledEntity);
            return activeModel;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("ModelEngine '" + modelId + "' konnte nicht angehaengt werden: " + rootMessage(exception));
            return null;
        }
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
