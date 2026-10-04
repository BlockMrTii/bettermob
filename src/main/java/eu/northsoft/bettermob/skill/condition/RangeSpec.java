package eu.northsoft.bettermob.skill.condition;

public final class RangeSpec {
    private RangeSpec() {}

    public static boolean matches(String spec, double value) {
        if (spec == null || spec.isBlank()) return false;
        String text = spec.trim();
        try {
            if (text.startsWith(">=")) return value >= Double.parseDouble(text.substring(2));
            if (text.startsWith("<=")) return value <= Double.parseDouble(text.substring(2));
            if (text.startsWith(">")) return value > Double.parseDouble(text.substring(1));
            if (text.startsWith("<")) return value < Double.parseDouble(text.substring(1));
            int dash = text.indexOf('-', 1);
            if (dash > 0) return value >= Double.parseDouble(text.substring(0, dash)) && value <= Double.parseDouble(text.substring(dash + 1));
            return Math.abs(value - Double.parseDouble(text)) < 0.5;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
