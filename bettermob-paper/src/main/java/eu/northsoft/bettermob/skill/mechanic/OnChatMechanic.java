package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class OnChatMechanic implements Mechanic {
    private static final int MAX_TICKS = 20 * 60 * 10;

    private final SkillEngine engine;

    public OnChatMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof Player player)) return;
        String skill = firstParam(call.params(), "onchat", "s", "skill");
        if (skill == null) return;
        int ticks = Math.max(1, Math.min(MAX_TICKS, parseInt(firstParam(call.params(), "d", "duration"), 200)));
        boolean cancel = !"false".equalsIgnoreCase(firstParam(call.params(), "cancel", "c"));
        engine.chatPrompts().await(player, stripQuotes(skill), call.context(), ticks, cancel);
    }
}
