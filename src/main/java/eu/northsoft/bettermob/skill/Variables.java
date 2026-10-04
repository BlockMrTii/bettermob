package eu.northsoft.bettermob.skill;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Variables {
    public static final int MAX_PER_SCOPE = 64;
    public static final String CASTER_PREFIX = "caster.";

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,32}");

    private static final Pattern UNSAFE = Pattern.compile("[{}\\[\\];=\"'\\\\%\\p{Cntrl}]");
    private static final Pattern LONE_HYPHEN = Pattern.compile("(?<=\\s)-(?=\\s)");

    private Variables() {}

    public static String sanitize(String value) {
        return LONE_HYPHEN.matcher(UNSAFE.matcher(value).replaceAll("")).replaceAll("");
    }

    public enum Type { INTEGER, FLOAT, STRING }

    public static boolean isCasterScope(String variable) {
        return variable.toLowerCase(Locale.ROOT).startsWith(CASTER_PREFIX);
    }

    public static String nameOf(String variable) {
        String name = isCasterScope(variable) ? variable.substring(CASTER_PREFIX.length()) : variable;
        return NAME.matcher(name).matches() ? name.toLowerCase(Locale.ROOT) : null;
    }

    public static Type typeOf(String value) {
        if (value == null) return Type.STRING;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "INTEGER", "INT" -> Type.INTEGER;
            case "FLOAT", "DOUBLE" -> Type.FLOAT;
            default -> Type.STRING;
        };
    }

    public static String normalise(String value, Type type) {
        String text = value.trim();
        try {
            return switch (type) {
                case INTEGER -> String.valueOf((long) Double.parseDouble(text));
                case FLOAT -> format(Double.parseDouble(text));
                case STRING -> value;
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String add(String current, String delta) {
        try {
            double sum = (current == null || current.isBlank() ? 0 : Double.parseDouble(current)) + Double.parseDouble(delta.trim());
            return format(sum);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static boolean put(Map<String, String> scope, String name, String value) {
        if (!scope.containsKey(name) && scope.size() >= MAX_PER_SCOPE) return false;
        scope.put(name, value);
        return true;
    }

    public static boolean matches(String spec, String stored) {
        if (spec == null) return false;
        Double number = null;
        if (stored == null || stored.isBlank()) number = 0.0;
        else {
            try {
                number = Double.parseDouble(stored.trim());
            } catch (NumberFormatException exception) {
                number = null;
            }
        }
        if (number != null && eu.northsoft.bettermob.skill.condition.RangeSpec.matches(spec, number)) return true;
        return stored != null && stored.trim().equalsIgnoreCase(spec.trim());
    }

    private static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? String.valueOf((long) value) : String.valueOf(value);
    }
}
