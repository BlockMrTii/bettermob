package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class GlowMechanic implements Mechanic {
    private record Active(boolean before, Object token) {}

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();
    private final SkillEngine engine;

    public GlowMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        UUID id = entity.getUniqueId();
        Object token = new Object();
        boolean before = active.containsKey(id) ? active.get(id).before() : entity.isGlowing();
        active.put(id, new Active(before, token));
        entity.setGlowing(true);
        int ticks = Math.max(1, parseInt(firstParam(call.params(), "d", "duration", "t"), 40));
        Tasks.runLater(engine.plugin(), entity, ticks, () -> {
            Active current = active.get(id);
            if (current == null || current.token() != token) return;
            active.remove(id);
            entity.setGlowing(before);
        });
    }
}
