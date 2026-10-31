package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.platform.ModelService;
import eu.northsoft.bettermob.platform.PlatformEntity;

import java.util.Map;

/** {@link ModelService} view of the Bukkit {@link BetterModelHook}. */
public final class BetterModelService implements ModelService {
    private final BetterModelHook hook;

    public BetterModelService(BetterModelHook hook) {
        this.hook = hook;
    }

    @Override
    public String id() {
        return "bettermodel";
    }

    @Override
    public boolean available() {
        return hook.available();
    }

    @Override
    public boolean hasModel(String modelId) {
        return hook.hasModel(modelId);
    }

    @Override
    public Object attach(PlatformEntity entity, String modelId) {
        return hook.attachIfPresent(PaperAdapters.bukkit(entity), modelId);
    }

    @Override
    public boolean play(Object model, String animation, Map<String, String> params) {
        return hook.play(model, animation);
    }

    @Override
    public boolean tint(Object model, String part, boolean exact, int rgb) {
        return hook.tint(model, part, exact, rgb);
    }

    @Override
    public boolean partVisible(Object model, String part, boolean exact, boolean visible) {
        return hook.partVisible(model, part, exact, visible);
    }

    @Override
    public void detach(Object model) {
        hook.close(model);
    }
}
