package eu.northsoft.bettermob.skill;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Params {
    private Params() {}

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

    public static String conditionParam(String paramsRaw, String... keys) {
        if (paramsRaw == null) return "";
        String value = firstParam(SkillStep.parseParams(paramsRaw), keys);
        return value == null ? "" : value.trim();
    }

    public static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }
}
