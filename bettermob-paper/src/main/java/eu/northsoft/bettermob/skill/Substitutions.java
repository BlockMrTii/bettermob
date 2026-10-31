package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.item.ItemNbt;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Substitutions {
    private Substitutions() {}

    static Map<String, String> resolveVariables(Map<String, String> params, Map<String, String> skillScope, Map<String, String> casterScope) {
        return resolveVariables(params, skillScope, casterScope, null);
    }

    static Map<String, String> resolveVariables(Map<String, String> params, Map<String, String> skillScope, Map<String, String> casterScope, Map<String, String> targetScope) {
        boolean needed = false;
        for (String value : params.values()) {
            if (value.indexOf("<skill.") >= 0 || value.indexOf("<var.") >= 0 || value.indexOf("<caster.var.") >= 0) {
                needed = true;
                break;
            }
        }
        if (!needed) return params;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.put(entry.getKey(), replaceVariables(entry.getValue(), skillScope, casterScope, targetScope, targetScope == null));
        }
        return result;
    }

    public static String replaceVariables(String value, Map<String, String> skillScope, Map<String, String> casterScope) {
        return replaceVariables(value, skillScope, casterScope, null);
    }

    public static String replaceVariables(String value, Map<String, String> skillScope, Map<String, String> casterScope, Map<String, String> targetScope) {
        return replaceVariables(value, skillScope, casterScope, targetScope, false);
    }

    private static String replaceVariables(String value, Map<String, String> skillScope, Map<String, String> casterScope, Map<String, String> targetScope, boolean keepTarget) {
        Matcher matcher = VARIABLE_PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String kind = matcher.group(1);
            if (keepTarget && kind.startsWith("target")) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            Map<String, String> scope = kind.startsWith("skill") ? skillScope : kind.startsWith("target") ? targetScope : casterScope;
            String found = scope == null ? null : scope.get(matcher.group(2).toLowerCase(Locale.ROOT));
            matcher.appendReplacement(result, Matcher.quoteReplacement(found == null ? "0" : Params.stripPlaceholderValue(found)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static final Pattern TARGET_VARIABLE = Pattern.compile("<target\\.var\\.([A-Za-z0-9_]{1,32})>");

    static String replaceTargetVariables(String value, Map<String, String> scope) {
        Matcher matcher = TARGET_VARIABLE.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String found = scope == null ? null : scope.get(matcher.group(1).toLowerCase(Locale.ROOT));
            matcher.appendReplacement(result, Matcher.quoteReplacement(found == null ? "0" : Params.stripPlaceholderValue(found)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static final Pattern VARIABLE_PLACEHOLDER = Pattern.compile("<(skill\\.var|skill|caster\\.var|target\\.var|var)\\.([A-Za-z0-9_]{1,32})>");

    private static boolean isInlineSkill(String value) {
        String trimmed = value.stripLeading();
        return trimmed.startsWith("[") && trimmed.indexOf('{') > 0;
    }

    private static final Pattern ITEM_NBT = Pattern.compile("<caster\\.itemnbt\\.([A-Za-z0-9_.-]{1,64})>");

    static String withItemNbt(String value, ItemStack held) {
        Matcher matcher = ITEM_NBT.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String found = ItemNbt.get(held, matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(found == null ? "" : Params.stripPlaceholderValue(found)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String plainNumber(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? String.valueOf((long) value) : String.valueOf(value);
    }

    public static String withLocationValues(String value, double x, double y, double z, float yaw, float pitch) {
        if (value.indexOf("<caster.l.") < 0) return value;
        return value.replace("<caster.l.x>", plainNumber(x)).replace("<caster.l.y>", plainNumber(y))
                .replace("<caster.l.z>", plainNumber(z)).replace("<caster.l.yaw>", plainNumber(yaw))
                .replace("<caster.l.pitch>", plainNumber(pitch));
    }

    static String withCasterValues(String value, String damage, String name, boolean escapesItself) {
        String replaced = value.replace("<caster.damage>", damage);
        if (escapesItself || isInlineSkill(value)) return replaced;
        return replaced.replace("<caster.name>", name);
    }
}
