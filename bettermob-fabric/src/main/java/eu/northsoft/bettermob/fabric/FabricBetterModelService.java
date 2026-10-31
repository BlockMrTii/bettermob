package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.ModelService;
import eu.northsoft.bettermob.platform.PlatformEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link ModelService} view of BetterModel on Fabric.
 */
public final class FabricBetterModelService implements ModelService {
    private static final String API = "kr.toxicity.model.api.BetterModel";
    private static final String FABRIC_ADAPTER = "kr.toxicity.model.api.fabric.platform.FabricAdapter";

    private volatile Method modelOrNullMethod;
    private volatile Method adaptMethod;
    private volatile Method animateMethod;

    @Override
    public String id() {
        return "bettermodel";
    }

    @Override
    public boolean available() {
        return FabricLoader.getInstance().isModLoaded("bettermodel") && classExists(API);
    }

    @Override
    public boolean hasModel(String modelId) {
        if (!available() || modelId == null) return false;
        try {
            return modelOrNullMethod().invoke(null, modelId) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public Object attach(PlatformEntity entity, String modelId) {
        if (!available() || entity == null || modelId == null) return null;
        try {
            Object renderer = modelOrNullMethod().invoke(null, modelId);
            if (renderer == null) return null;
            Entity mcEntity = FabricAdapters.minecraft(entity);
            Object adapted = adapt(mcEntity);
            if (adapted == null) return null;
            for (Method method : renderer.getClass().getMethods()) {
                if (method.getName().equals("getOrCreate") && method.getParameterCount() == 1) {
                    try {
                        return method.invoke(renderer, adapted);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private Object adapt(Entity entity) {
        try {
            return adaptMethod().invoke(null, entity);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Override
    public boolean play(Object model, String animation, Map<String, String> params) {
        if (model == null || animation == null) return false;
        try {
            Method m = animateMethod != null ? animateMethod : (animateMethod = model.getClass().getMethod("animate", String.class));
            m.invoke(model, animation);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public boolean tint(Object model, String part, boolean exact, int rgb) {
        return false;
    }

    @Override
    public boolean partVisible(Object model, String part, boolean exact, boolean visible) {
        return false;
    }

    @Override
    public void detach(Object model) {
        if (model == null) return;
        try {
            model.getClass().getMethod("close").invoke(model);
        } catch (ReflectiveOperationException | LinkageError ignored) {}
    }

    private Method modelOrNullMethod() throws ReflectiveOperationException {
        if (modelOrNullMethod == null) {
            modelOrNullMethod = Class.forName(API).getMethod("modelOrNull", String.class);
        }
        return modelOrNullMethod;
    }

    private Method adaptMethod() throws ReflectiveOperationException {
        if (adaptMethod == null) {
            Class<?> adapterClass;
            try {
                adapterClass = Class.forName(FABRIC_ADAPTER);
            } catch (ClassNotFoundException e) {
                adapterClass = Class.forName("kr.toxicity.model.api.fabric.FabricAdapter");
            }
            adaptMethod = adapterClass.getMethod("adapt", Entity.class);
        }
        return adaptMethod;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
