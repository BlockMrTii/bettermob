package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.integration.PlaceholderHook;
import eu.northsoft.bettermob.skill.Arguments;
import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConditionParams {
    private static final Pattern PLACEHOLDER = Pattern.compile("<placeholder\\.([^<>\\s{}\\[\\];=\"']{1,64})>");

    private ConditionParams() {}

    public static String resolve(SkillEngine engine, String params, SkillContext context, Target override) {
        if (params == null || params.indexOf('<') < 0) return params;
        String text = engine == null ? Arguments.resolve(params, context.arguments(), name -> { }) : engine.argumentValue(params, context);
        if (engine != null && !engine.placeholders().isEmpty()) text = engine.placeholders().apply(text, context);
        Entity target = override != null && override.entity() != null ? override.entity() : context.trigger();
        Map<String, String> caster = engine == null || context.caster() == null ? null : engine.state().existingVariablesOf(context.caster().getUniqueId());
        Map<String, String> targetScope = engine == null || target == null ? null : engine.state().existingVariablesOf(target.getUniqueId());
        text = SkillEngine.replaceVariables(text, context.variables(), caster, targetScope);
        if (context.caster() != null && text.indexOf("<caster.name>") >= 0) text = text.replace("<caster.name>", Params.stripPlaceholderValue(context.caster().getName()));
        if (target != null && text.indexOf("<target.name>") >= 0) text = text.replace("<target.name>", Params.stripPlaceholderValue(target.getName()));
        if (text.indexOf("<placeholder.") >= 0) text = papi(text, playerOf(context, target));
        return text;
    }

    static String papi(String text, Player player) {
        return papi(text, key -> PlaceholderHook.apply(player, "%" + key + "%"));
    }

    static String papi(String text, Function<String, String> lookup) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = lookup.apply(matcher.group(1));
            String replacement = value == null || value.equals("%" + matcher.group(1) + "%") ? "" : Params.stripPlaceholderValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Player playerOf(SkillContext context, Entity target) {
        if (target instanceof Player player) return player;
        if (context.trigger() instanceof Player player) return player;
        return context.caster() instanceof Player player ? player : null;
    }
}
