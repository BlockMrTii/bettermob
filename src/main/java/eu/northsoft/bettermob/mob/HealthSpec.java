package eu.northsoft.bettermob.mob;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HealthSpec(String operator, double value, boolean percent) {
    private static final Pattern PATTERN = Pattern.compile("^(<=|>=|<|>)(\\d+(?:\\.\\d+)?)(%)?$");

    public static HealthSpec parse(String operator, String number, String percent) {
        if (operator == null || number == null) return null;
        Matcher matcher = PATTERN.matcher(operator + number + (percent == null ? "" : percent));
        if (!matcher.matches()) return null;
        return new HealthSpec(matcher.group(1), Double.parseDouble(matcher.group(2)), matcher.group(3) != null);
    }

    public boolean matches(double health, double maxHealth) {
        double actual = percent ? (maxHealth <= 0 ? 0 : health / maxHealth * 100) : health;
        return switch (operator) {
            case "<" -> actual < value;
            case "<=" -> actual <= value;
            case ">" -> actual > value;
            default -> actual >= value;
        };
    }
}
