package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.World;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class ChunkForceLoadMechanic implements Mechanic {
    private final SkillEngine engine;

    public ChunkForceLoadMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        boolean load = !"false".equalsIgnoreCase(firstParam(call.params(), "l", "load", "loaded"));
        World world = at.getWorld();
        int chunkX = at.getBlockX() >> 4;
        int chunkZ = at.getBlockZ() >> 4;
        Tasks.runGlobal(engine.plugin(), () -> world.setChunkForceLoaded(chunkX, chunkZ, load));
    }
}
