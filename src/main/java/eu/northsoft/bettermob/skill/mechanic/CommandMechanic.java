package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.integration.PlaceholderHook;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class CommandMechanic implements Mechanic {
    private final SkillEngine engine;

    public CommandMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        SkillContext context = call.context();
        String raw = call.params().get("c");
        if (raw == null) return;
        String targetName = context.trigger() != null ? context.trigger().getName() : context.caster().getName();
        String command = PlaceholderHook.apply(placeholderPlayer(context), stripQuotes(raw)
                .replace("<caster.name>", asArgument(context.caster().getName()))
                .replace("<target.name>", asArgument(targetName)));
        Tasks.runGlobal(engine.plugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    static String asArgument(String name) {
        return name.replace(' ', '_').replaceAll("[^A-Za-z0-9_.]", "");
    }

    private static Player placeholderPlayer(SkillContext context) {
        if (context.trigger() instanceof Player player) return player;
        return context.caster() instanceof Player player ? player : null;
    }
}
