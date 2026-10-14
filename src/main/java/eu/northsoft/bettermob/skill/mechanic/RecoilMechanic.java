package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class RecoilMechanic implements Mechanic {
    private static final float MAX = 5f;

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        float raw = parseFloat(firstParam(call.params(), "f", "force", "v"), 1f);
        if (!Float.isFinite(raw)) return;
        float force = Math.max(-MAX, Math.min(MAX, raw));
        Vector back = entity.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 1.0e-9) return;
        entity.setVelocity(entity.getVelocity().add(back.normalize().multiply(-force)));
    }
}
