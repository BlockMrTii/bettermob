package eu.northsoft.bettermob.platform;

import java.util.Map;

/**
 * Attaches and drives 3D models (BetterModel, ModelEngine, ...). The returned model handle is
 * opaque to the core and only passed back into this service.
 */
public interface ModelService {
    /** Short id, e.g. {@code bettermodel}. */
    String id();

    boolean available();

    boolean hasModel(String modelId);

    /** Attaches the model; {@code null} if unavailable or unknown. */
    Object attach(PlatformEntity entity, String modelId);

    boolean play(Object model, String animation, Map<String, String> params);

    boolean tint(Object model, String part, boolean exact, int rgb);

    boolean partVisible(Object model, String part, boolean exact, boolean visible);

    void detach(Object model);
}
