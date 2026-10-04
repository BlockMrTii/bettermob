package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.Aura;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class AuraMechanic implements Mechanic {
    private final SkillEngine engine;

    public AuraMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        String name = firstParam(p, "auraname", "name", "aura");
        if (name == null || !(call.target().entity() instanceof LivingEntity entity) || !entity.isValid()) return;
        String key = name.toLowerCase(Locale.ROOT);
        int ticks = parseInt(firstParam(p, "time", "ticks", "duration"), 0);
        long until = ticks > 0 ? System.currentTimeMillis() + ticks * 50L : Long.MAX_VALUE;
        Aura aura = new Aura(call.step().name(), until, Boolean.parseBoolean(firstParam(p, "ce", "cancelevent")),
                firstParam(p, "oe", "onend"), firstParam(p, "oh", "onhit"));

        Map<String, Aura> active = engine.state().aurasOf(entity.getUniqueId());
        Aura old = active.put(key, aura);
        if (old != null) old.stop();

        runLines(firstParam(p, "os", "onstart"), SkillContext.of(entity));

        String onTick = firstParam(p, "ot", "ontick");
        if (onTick != null) {
            long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 20));
            aura.cancelTicker = Tasks.runTimer(engine.plugin(), entity, interval, interval, () -> {
                if (entity.isDead() || active.get(key) != aura) aura.cancelTicker.run();
                else runLines(onTick, SkillContext.of(entity));
            });
        }
        if (ticks > 0) {
            Tasks.runLater(engine.plugin(), entity, ticks, () -> end(entity, active, key, aura));
        }
    }

    private void end(LivingEntity entity, Map<String, Aura> active, String key, Aura aura) {
        if (!active.remove(key, aura)) return;
        aura.stop();
        runLines(aura.onEnd, SkillContext.of(entity));
    }

    private void runLines(String lines, SkillContext context) {
        if (lines != null) engine.runInline(lines, context);
    }
}
