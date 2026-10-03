package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class VelocityMechanic implements Mechanic {
    private final SkillEngine engine;

    public VelocityMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Entity entity = call.target().entity();
        if (entity == null) return;
        String mode = p.getOrDefault("m", p.getOrDefault("mode", "SET")).trim().toUpperCase(Locale.ROOT);
        Vector change = new Vector(parseFloat(p.get("x"), 0f), parseFloat(p.get("y"), 0f), parseFloat(p.get("z"), 0f));
        long interval = Math.max(1, parseInt(firstParam(p, "repeatinterval", "ri"), 1));
        apply(entity, mode, change);
        int repeat = parseInt(p.get("repeat"), 0);
        for (int i = 1; i <= repeat; i++) {
            Tasks.runLater(engine.plugin(), entity, i * interval, () -> apply(entity, mode, change));
        }
    }

    private static void apply(Entity entity, String mode, Vector change) {
        Vector current = entity.getVelocity();
        entity.setVelocity(switch (mode) {
            case "ADD" -> current.add(change);
            case "MULTIPLY" -> current.multiply(change);
            case "DIVIDE" -> new Vector(div(current.getX(), change.getX()), div(current.getY(), change.getY()), div(current.getZ(), change.getZ()));
            default -> change.clone();
        });
    }

    private static double div(double value, double divisor) {
        return divisor == 0 ? value : value / divisor;
    }
}
