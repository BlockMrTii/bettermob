package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class RotationMechanic implements Mechanic {
    private final boolean pitchOnly;

    public RotationMechanic(boolean pitchOnly) {
        this.pitchOnly = pitchOnly;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        boolean relative = "true".equalsIgnoreCase(stripQuotes(call.params().getOrDefault("relative", "false")));
        float currentYaw = entity.getYaw();
        float currentPitch = entity.getPitch();
        float yaw = pitchOnly ? currentYaw : number(call.params().get("yaw"), currentYaw, relative, currentYaw);
        float pitch = number(pitchOnly ? firstParam(call.params(), "p", "pitch") : call.params().get("pitch"), currentPitch, relative, currentPitch);
        entity.setRotation(yaw, Math.max(-90f, Math.min(90f, pitch)));
    }

    private static float number(String raw, float fallback, boolean relative, float current) {
        if (raw == null) return fallback;
        float value = parseFloat(stripQuotes(raw), Float.NaN);
        if (!Float.isFinite(value)) return fallback;
        return relative ? current + value : value;
    }
}
