package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class ActionBarMechanic implements Mechanic {
    private static final int REPEAT_TICKS = 20;

    private final SkillEngine engine;

    public ActionBarMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        var text = MessageText.render(raw, receiver, call.context().caster().getName());
        receiver.sendActionBar(text);
        int duration = MessageText.ticks(firstParam(call.params(), "d", "duration"), 0);
        for (int elapsed = REPEAT_TICKS; elapsed < duration; elapsed += REPEAT_TICKS) {
            Tasks.runLater(engine.plugin(), receiver, elapsed, () -> receiver.sendActionBar(text));
        }
    }
}
