package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Variables;
import org.bukkit.entity.Player;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class KillCountMechanic implements Mechanic {
    private final SkillEngine engine;

    public KillCountMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Player player = call.target().entity() instanceof Player target ? target
                : call.context().trigger() instanceof Player trigger ? trigger
                : call.context().caster() instanceof Player caster ? caster : null;
        if (player == null || engine.plugin().kills() == null) return;
        String variable = firstParam(call.params(), "var", "variable", "name");
        String name = variable == null ? null : Variables.nameOf(variable);
        if (name == null) {
            engine.plugin().messages().warn("skill.variableName", "variable", variable);
            return;
        }
        String mob = firstParam(call.params(), "mob", "id", "m");
        int kills = mob == null ? engine.plugin().kills().total(player.getUniqueId()) : engine.plugin().kills().count(player.getUniqueId(), mob);
        SkillContext context = call.context();
        Map<String, String> scope = Variables.isCasterScope(variable)
                ? engine.state().variablesOf(context.caster().getUniqueId()) : context.variables();
        if (!Variables.put(scope, name, String.valueOf(kills))) {
            engine.plugin().messages().warn("skill.variableLimit", "variable", variable, "limit", Variables.MAX_PER_SCOPE);
        }
    }
}
