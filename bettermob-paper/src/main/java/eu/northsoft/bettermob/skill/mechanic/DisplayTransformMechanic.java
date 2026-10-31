package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseNumber;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class DisplayTransformMechanic implements Mechanic {
    private static final float LIMIT = 64f;

    static Vector3f vector(String raw) {
        if (raw == null) return null;
        String[] parts = stripQuotes(raw).split(",");
        if (parts.length != 3) return null;
        float[] values = new float[3];
        for (int i = 0; i < 3; i++) {
            double value = parseNumber(parts[i].trim(), Double.NaN);
            if (Double.isNaN(value)) return null;
            values[i] = (float) Math.max(-LIMIT, Math.min(LIMIT, value));
        }
        return new Vector3f(values[0], values[1], values[2]);
    }

    static Vector3f combine(Vector3f current, Vector3f value, String action) {
        return switch (action) {
            case "ADD" -> new Vector3f(current).add(value);
            case "MULTIPLY" -> new Vector3f(current).mul(value);
            default -> new Vector3f(value);
        };
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (!(entity instanceof Display display)) return;
        Vector3f value = vector(firstParam(call.params(), "value", "val", "v"));
        if (value == null) return;
        String action = String.valueOf(firstParam(call.params(), "action", "a")).toUpperCase(Locale.ROOT);
        String part = String.valueOf(firstParam(call.params(), "transformation", "t")).toLowerCase(Locale.ROOT);
        Transformation now = display.getTransformation();
        Transformation next = switch (part) {
            case "scale" -> new Transformation(now.getTranslation(), now.getLeftRotation(), combine(now.getScale(), value, action), now.getRightRotation());
            case "translation" -> new Transformation(combine(now.getTranslation(), value, action), now.getLeftRotation(), now.getScale(), now.getRightRotation());
            default -> null;
        };
        if (next != null) display.setTransformation(next);
    }
}
