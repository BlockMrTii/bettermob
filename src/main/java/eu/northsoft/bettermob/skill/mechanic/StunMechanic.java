package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class StunMechanic implements Mechanic {
    private record State(boolean aware, boolean gravity, int count) {}

    private final Map<UUID, State> active = new ConcurrentHashMap<>();
    private final SkillEngine engine;

    public StunMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        if (!(call.target().entity() instanceof Mob mob)) return;
        int ticks = parseInt(firstParam(p, "d", "duration", "t"), 20);
        boolean ai = !"false".equalsIgnoreCase(p.get("ai"));
        boolean gravity = "true".equalsIgnoreCase(p.get("g"));
        boolean freeze = "true".equalsIgnoreCase(p.get("f"));

        String animation = firstParam(p, "state", "animation", "s");
        if (animation != null) engine.betterModel().play(engine.mobManager().trackerFor(mob.getUniqueId()), animation);

        UUID id = mob.getUniqueId();
        State current = active.get(id);
        active.put(id, current == null
                ? new State(mob.isAware(), mob.hasGravity(), 1)
                : new State(current.aware(), current.gravity(), current.count() + 1));
        if (ai) mob.setAware(false);
        if (gravity) mob.setGravity(false);
        Runnable[] cancelFreeze = {() -> { }};
        if (freeze) {
            cancelFreeze[0] = Tasks.runTimer(engine.plugin(), mob, 1L, 1L, () -> {
                if (!mob.isValid()) cancelFreeze[0].run();
                else mob.setVelocity(new Vector());
            });
        }
        Tasks.runLater(engine.plugin(), mob, ticks, () -> {
            cancelFreeze[0].run();
            State latest = active.get(id);
            if (latest == null) return;
            if (latest.count() <= 1) {
                active.remove(id);
                mob.setAware(latest.aware());
                mob.setGravity(latest.gravity());
            } else {
                active.put(id, new State(latest.aware(), latest.gravity(), latest.count() - 1));
            }
        }, () -> {
            cancelFreeze[0].run();
            active.remove(id);
        });
    }
}
