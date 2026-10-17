package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.skill.condition.RangeSpec;
import org.bukkit.entity.Entity;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Variables {
    public static final int MAX_PER_SCOPE = 64;
    public static final String CASTER_PREFIX = "caster.";
    public static final String TARGET_PREFIX = "target.";

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,32}");

    private Variables() {}

    public enum Type { INTEGER, FLOAT, STRING }

    public static boolean isCasterScope(String variable) {
        return variable.toLowerCase(Locale.ROOT).startsWith(CASTER_PREFIX);
    }

    public static boolean isTargetScope(String variable) {
        return variable.toLowerCase(Locale.ROOT).startsWith(TARGET_PREFIX);
    }

    public static String nameOf(String variable) {
        String name = isCasterScope(variable) ? variable.substring(CASTER_PREFIX.length())
                : isTargetScope(variable) ? variable.substring(TARGET_PREFIX.length()) : variable;
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
                case INTEGER -> {
                    double number = Double.parseDouble(text);
                    yield Double.isFinite(number) ? String.valueOf((long) number) : null;
                }
                case FLOAT -> {
                    double number = Double.parseDouble(text);
                    yield Double.isFinite(number) ? format(number) : null;
                }
                case STRING -> value;
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String add(String current, String delta) {
        try {
            double sum = (current == null || current.isBlank() ? 0 : Double.parseDouble(current)) + Double.parseDouble(delta.trim());
            return Double.isFinite(sum) ? format(sum) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String subtract(String current, String delta) {
        try {
            return add(current, format(-Double.parseDouble(delta.trim())));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static boolean inRange(String stored, String spec) {
        if (spec == null || spec.isBlank()) return false;
        double number;
        try {
            number = stored == null || stored.isBlank() ? 0 : Double.parseDouble(stored.trim());
        } catch (NumberFormatException exception) {
            return false;
        }
        String text = spec.trim().toLowerCase(Locale.ROOT);
        int to = text.indexOf("to", 1);
        if (to > 0) {
            try {
                return number >= Double.parseDouble(text.substring(0, to).trim()) && number <= Double.parseDouble(text.substring(to + 2).trim());
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return RangeSpec.matches(text, number);
    }

    public static Map<String, String> scopeOf(String variable, SkillContext context, Entity target, SkillState state, boolean create) {
        if (isCasterScope(variable)) return create ? state.variablesOf(context.caster().getUniqueId()) : state.existingVariablesOf(context.caster().getUniqueId());
        if (isTargetScope(variable)) {
            Entity entity = target != null ? target : context.trigger();
            if (entity == null) return null;
            return create ? state.variablesOf(entity.getUniqueId()) : state.existingVariablesOf(entity.getUniqueId());
        }
        return context.variables();
    }

    public static String formatNumber(double value) {
        return format(value);
    }

    public static boolean put(Map<String, String> scope, String name, String value) {
        synchronized (scope) {
            if (!scope.containsKey(name) && scope.size() >= MAX_PER_SCOPE) return false;
            scope.put(name, value);
            return true;
        }
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
        if (number != null && RangeSpec.matches(spec, number)) return true;
        return stored != null && stored.trim().equalsIgnoreCase(spec.trim());
    }

    private static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? String.valueOf((long) value) : String.valueOf(value);
    }
}
