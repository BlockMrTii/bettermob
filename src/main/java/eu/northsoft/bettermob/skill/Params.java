package eu.northsoft.bettermob.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class Params {
    private static final Pattern SKILL_SYNTAX = Pattern.compile("[{}\\[\\];=\"'\\\\\\p{Cntrl}-]");

    private Params() {}

    private static final Pattern PLACEHOLDER_SYNTAX = Pattern.compile("[{}\\[\\];=\"'\\\\%\\p{Cntrl}]");
    private static final Pattern LONE_HYPHEN = Pattern.compile("(?<=\\s)-(?=\\s)");

    public static String stripPlaceholderValue(String value) {
        return LONE_HYPHEN.matcher(PLACEHOLDER_SYNTAX.matcher(value).replaceAll("")).replaceAll("");
    }

    public static Map<String, String> parsedParams(String paramsRaw) {
        if (paramsRaw == null) return Map.of();
        Map<String, String> parsed = PARSED_PARAMS.get(paramsRaw);
        if (parsed == null) {
            parsed = SkillStep.parseParams(paramsRaw);
            if (PARSED_PARAMS.size() >= PARAMS_CACHE_LIMIT) PARSED_PARAMS.clear();
            PARSED_PARAMS.put(paramsRaw, parsed);
        }
        return parsed;
    }

    public static String stripSkillSyntax(String value) {
        return SKILL_SYNTAX.matcher(value).replaceAll("");
    }

    public static float parseFloat(String value, float fallback) {
        try {
            return value == null ? fallback : Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static String firstParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (value != null) return value;
        }
        return null;
    }

    public static Map<String, String> without(Map<String, String> params, String key) {
        Map<String, String> copy = new LinkedHashMap<>(params);
        copy.remove(key);
        return copy;
    }

    private static final int PARAMS_CACHE_LIMIT = 4096;
    private static final Map<String, Map<String, String>> PARSED_PARAMS = new ConcurrentHashMap<>();

    public static String conditionParam(String paramsRaw, String... keys) {
        if (paramsRaw == null) return "";
        Map<String, String> parsed = parsedParams(paramsRaw);
        String value = firstParam(parsed, keys);
        return value == null ? "" : value.trim();
    }

    public static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }
}
