package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseNumber;

public final class TeleportYMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        double y = parseNumber(firstParam(call.params(), "y"), Double.NaN);
        if (Double.isNaN(y)) return;
        Location to = entity.getLocation();
        to.setY(Math.max(to.getWorld().getMinHeight() - 64, Math.min(to.getWorld().getMaxHeight() + 64, y)));
        entity.teleportAsync(to);
    }
}
