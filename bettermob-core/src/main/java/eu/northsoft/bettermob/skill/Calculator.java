package eu.northsoft.bettermob.skill;

public final class Calculator {
    private static final int MAX_LENGTH = 200;
    private static final int MAX_DEPTH = 32;

    private final String text;
    private int position;
    private int depth;

    private Calculator(String text) {
        this.text = text;
    }

    public static Double evaluate(String expression) {
        if (expression == null) return null;
        String trimmed = expression.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) return null;
        try {
            Calculator calculator = new Calculator(trimmed);
            double value = calculator.sum();
            calculator.skipSpaces();
            if (calculator.position != trimmed.length() || !Double.isFinite(value)) return null;
            return value;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void skipSpaces() {
        while (position < text.length() && text.charAt(position) == ' ') position++;
    }

    private boolean accept(char symbol) {
        skipSpaces();
        if (position < text.length() && text.charAt(position) == symbol) {
            position++;
            return true;
        }
        return false;
    }

    private double sum() {
        double value = product();
        while (true) {
            if (accept('+')) value += product();
            else if (accept('-')) value -= product();
            else return value;
        }
    }

    private double product() {
        double value = unary();
        while (true) {
            if (accept('*')) value *= unary();
            else if (accept('/')) {
                double divisor = unary();
                if (divisor == 0) throw new IllegalArgumentException("division by zero");
                value /= divisor;
            } else if (accept('%')) {
                double divisor = unary();
                if (divisor == 0) throw new IllegalArgumentException("division by zero");
                value %= divisor;
            } else return value;
        }
    }

    private double unary() {
        if (accept('-')) return -unary();
        if (accept('+')) return unary();
        return atom();
    }

    private double atom() {
        if (++depth > MAX_DEPTH) throw new IllegalArgumentException("too deep");
        try {
            if (accept('(')) {
                double value = sum();
                if (!accept(')')) throw new IllegalArgumentException("missing )");
                return value;
            }
            skipSpaces();
            int start = position;
            while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) position++;
            if (start == position) throw new IllegalArgumentException("number expected");
            return Double.parseDouble(text.substring(start, position));
        } finally {
            depth--;
        }
    }
}
