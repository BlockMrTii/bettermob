package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class ScaleMechanic implements Mechanic {
    private static final float MIN = 0.0625f;
    private static final float MAX = 16f;

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        String raw = firstParam(call.params(), "s", "scale", "value");
        if (entity == null || raw == null) return;
        float scale = parseFloat(stripQuotes(raw), Float.NaN);
        if (!Float.isFinite(scale)) return;
        scale = Math.max(MIN, Math.min(MAX, scale));
        if (entity instanceof Display display) {
            Transformation old = display.getTransformation();
            display.setTransformation(new Transformation(old.getTranslation(), old.getLeftRotation(), new org.joml.Vector3f(scale, scale, scale), old.getRightRotation()));
        } else if (entity instanceof LivingEntity living) {
            AttributeInstance attribute = living.getAttribute(Attribute.SCALE);
            if (attribute != null) attribute.setBaseValue(scale);
        }
    }
}
