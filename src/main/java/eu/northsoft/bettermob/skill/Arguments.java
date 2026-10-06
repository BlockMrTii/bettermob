package eu.northsoft.bettermob.skill;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Arguments {
    private static final Set<String> RESERVED = Set.of("s", "skill", "skills", "sync", "delay", "cd");
    private static final Pattern PLACEHOLDER = Pattern.compile("<arg\\.([A-Za-z0-9_]{1,32})>");

    private Arguments() {}

    public static Map<String, String> from(Map<String, String> params) {
        Map<String, String> arguments = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!RESERVED.contains(entry.getKey())) arguments.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return arguments;
    }

    public static Map<String, String> withDefaults(Map<String, String> given, Map<String, String> defaults) {
        if (defaults.isEmpty()) return given;
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        merged.putAll(given);
        return merged;
    }

    public static boolean mentions(Map<String, String> params) {
        for (String value : params.values()) {
            if (value.indexOf("<arg.") >= 0) return true;
        }
        return false;
    }

    public static String resolve(String value, Map<String, String> arguments, java.util.function.Consumer<String> missing) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            String found = arguments.get(name);
            if (found == null) missing.accept(name);
            matcher.appendReplacement(result, Matcher.quoteReplacement(found == null ? matcher.group(0) : Params.stripPlaceholderValue(found)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
