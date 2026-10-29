package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class TeleportToMechanic implements Mechanic {
    private final SkillEngine engine;

    public TeleportToMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        Location destination = engine.locationParam(firstParam(call.params(), "location", "l", "loc"), call.context());
        if (destination == null) return;
        Tasks.runOwned(engine.plugin(), entity, () -> {
            Location to = destination.clone();
            to.setYaw(entity.getLocation().getYaw());
            to.setPitch(entity.getLocation().getPitch());
            entity.teleportAsync(to);
        });
    }
}
