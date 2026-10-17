package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class ChunkForceLoadMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        boolean load = !"false".equalsIgnoreCase(firstParam(call.params(), "l", "load", "loaded"));
        at.getChunk().setForceLoaded(load);
    }
}
