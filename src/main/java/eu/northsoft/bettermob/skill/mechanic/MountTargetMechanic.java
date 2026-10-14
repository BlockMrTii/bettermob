package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class MountTargetMechanic implements Mechanic {
    private static final int MAX_REPEAT = 40;

    private final SkillEngine engine;

    public MountTargetMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity rider = call.target().entity();
        Entity vehicle = call.context().caster();
        if (rider == null || rider.equals(vehicle)) return;
        int repeat = Math.max(0, Math.min(MAX_REPEAT, parseInt(call.params().get("repeat"), 0)));
        long interval = Math.max(1, parseInt(firstParam(call.params(), "repeati", "repeatinterval"), 1));
        mount(vehicle, rider);
        for (int i = 1; i <= repeat; i++) {
            Tasks.runLater(engine.plugin(), rider, i * interval, () -> mount(vehicle, rider));
        }
    }

    private static void mount(Entity vehicle, Entity rider) {
        if (!Bukkit.isOwnedByCurrentRegion(vehicle) || !Bukkit.isOwnedByCurrentRegion(rider)) return;
        if (rider.isInsideVehicle() || !vehicle.isValid() || !rider.isValid()) return;
        vehicle.addPassenger(rider);
    }
}
