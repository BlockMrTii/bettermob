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
    private record State(boolean aware, boolean gravity, int aiCount, int gravityCount) {}

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
        if (ai || gravity) {
            State current = active.get(id);
            boolean baseAware = current == null || current.aiCount() == 0 ? mob.isAware() : current.aware();
            boolean baseGravity = current == null || current.gravityCount() == 0 ? mob.hasGravity() : current.gravity();
            int aiCount = (current == null ? 0 : current.aiCount()) + (ai ? 1 : 0);
            int gravityCount = (current == null ? 0 : current.gravityCount()) + (gravity ? 1 : 0);
            active.put(id, new State(baseAware, baseGravity, aiCount, gravityCount));
        }
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
            int aiCount = ai ? Math.max(0, latest.aiCount() - 1) : latest.aiCount();
            int gravityCount = gravity ? Math.max(0, latest.gravityCount() - 1) : latest.gravityCount();
            if (ai && aiCount == 0) mob.setAware(latest.aware());
            if (gravity && gravityCount == 0) mob.setGravity(latest.gravity());
            if (aiCount == 0 && gravityCount == 0) active.remove(id);
            else active.put(id, new State(latest.aware(), latest.gravity(), aiCount, gravityCount));
        }, () -> {
            cancelFreeze[0].run();
            active.remove(id);
        });
    }
}
